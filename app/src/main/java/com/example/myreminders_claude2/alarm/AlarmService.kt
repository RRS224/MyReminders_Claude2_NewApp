package com.example.myreminders_claude2.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.util.Log
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.myreminders_claude2.MainActivity
import com.example.myreminders_claude2.R
import com.example.myreminders_claude2.data.ReminderDatabase
import com.example.myreminders_claude2.data.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmService : Service(), TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var textToSpeech: TextToSpeech? = null
    private var vibrator: Vibrator? = null
    private var isVoiceEnabled: Boolean = true
    private var reminderText: String = ""
    private var currentReminderId: Long = -1
    private var currentSnoozeCount: Int = 0
    private val handler: Handler = Handler(Looper.getMainLooper())
    // ✅ P1 #9: Prevent race condition if dismiss fires from two paths simultaneously
    private val isDismissing = java.util.concurrent.atomic.AtomicBoolean(false)

    // ✅ P1 #5 & #8: Safe Long→Int conversion for notification IDs and request codes
    private fun Long.toSafeInt(): Int = (this and 0x7FFFFFFF).toInt()

    companion object {
        const val CHANNEL_ID = "reminder_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_DISMISS = "com.example.myreminders_claude2.ACTION_DISMISS"
        const val ACTION_STOP_ALARM = "com.example.myreminders_claude2.ACTION_STOP_ALARM"
        private const val MAX_AUTO_SNOOZES = 2
        private const val RING_DURATION = 30 * 1000L
        private const val SNOOZE_INTERVAL = 5 * 60 * 1000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        textToSpeech = TextToSpeech(this, this)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action: String? = intent?.action

        return when (action) {
            ACTION_DISMISS -> {
                handleDismiss(isManual = true)
                START_NOT_STICKY
            }
            ACTION_STOP_ALARM -> {
                Log.d("AlarmService", "=== ACTION_STOP_ALARM RECEIVED ===")
                // Just stop the alarm, don't open MainActivity
                // The user can use notification buttons or swipe to dismiss
                stopAlarmSound()  // This already stops vibration
                Log.d("AlarmService", "Alarm sound stopped")
                stopForeground(STOP_FOREGROUND_REMOVE)
                Log.d("AlarmService", "Stopped foreground")
                stopSelf()
                Log.d("AlarmService", "Service stopped")
                START_NOT_STICKY
            }
            else -> {
                currentReminderId = intent?.getLongExtra("REMINDER_ID", -1) ?: -1
                currentSnoozeCount = intent?.getIntExtra("SNOOZE_COUNT", 0) ?: 0
                val title: String = intent?.getStringExtra("REMINDER_TITLE") ?: "Reminder"
                val notes: String = intent?.getStringExtra("REMINDER_NOTES") ?: ""
                isVoiceEnabled = intent?.getBooleanExtra("VOICE_ENABLED", true) ?: true

                reminderText = if (notes.isNotBlank()) "$title. $notes" else title

                // ✅ Show "Second Reminder" / "Third Reminder" in notification
                val notification = createNotification(title, notes, currentReminderId, currentSnoozeCount)
                startForeground(currentReminderId.toSafeInt(), notification)  // ✅ P1 #8: unique ID per reminder

                playAlarmSound()
                startVibration()
                scheduleAutoSnooze()

                START_NOT_STICKY
            }
        }
    }

    private fun scheduleAutoSnooze() {
        handler.postDelayed({
            CoroutineScope(Dispatchers.IO).launch {
                val database = ReminderDatabase.getDatabase(applicationContext)
                val reminder = database.reminderDao().getReminderById(currentReminderId)

                reminder?.let {
                    if (it.snoozeCount < MAX_AUTO_SNOOZES) {
                        performAutoSnooze()
                    } else {
                        handleDismiss(isManual = false)
                    }
                }
            }
        }, RING_DURATION)
    }

    private fun performAutoSnooze() {
        stopAlarmSound()

        CoroutineScope(Dispatchers.IO).launch {
            val database = ReminderDatabase.getDatabase(applicationContext)
            val reminder = database.reminderDao().getReminderById(currentReminderId)

            reminder?.let {
                val newSnoozeCount: Int = it.snoozeCount + 1
                val newDateTime: Long = System.currentTimeMillis() + SNOOZE_INTERVAL

                database.reminderDao().updateSnoozeCount(currentReminderId, newSnoozeCount)

                val updated = it.copy(
                    dateTime = newDateTime,
                    snoozeCount = newSnoozeCount
                )
                database.reminderDao().updateReminder(updated)

                val alarmScheduler = AlarmScheduler(applicationContext)
                alarmScheduler.scheduleAlarm(updated)
            }
        }
    }

    private fun handleDismiss(isManual: Boolean) {
        // ✅ P1 #9: Prevent double-dismiss race condition
        if (!isDismissing.compareAndSet(false, true)) {
            android.util.Log.w("AlarmService", "handleDismiss already in progress — ignoring duplicate call")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val database = ReminderDatabase.getDatabase(applicationContext)
            val completedAt: Long = System.currentTimeMillis()
            val dismissalReason: String = if (isManual) "MANUAL" else "AUTO_SNOOZED"

            if (isManual) {
                // ✅ Manually dismissed → goes to Done tab
                database.reminderDao().softDeleteReminder(currentReminderId, completedAt)
                // ✅ P1 #9: Sync deletion to Firestore (DAO-direct calls bypass SyncManager)
                val syncManager = SyncManager(
                    context = applicationContext,
                    firestore = FirebaseFirestore.getInstance(),
                    auth = FirebaseAuth.getInstance(),
                    reminderDao = database.reminderDao(),
                    categoryDao = database.categoryDao(),
                    templateDao = database.templateDao()
                )
                database.reminderDao().getReminderByIdSync(currentReminderId)?.let {
                    syncManager.uploadReminder(it)
                }
            } else {
                // ✅ Unattended (rang 3 times, ignored) → goes to Missed tab
                database.reminderDao().markAsCompleted(
                    currentReminderId,
                    true,
                    completedAt,
                    dismissalReason
                )
                // ✅ Update updatedAt so Firebase sync doesn't revert it
                val reminder = database.reminderDao().getReminderByIdSync(currentReminderId)
                reminder?.let {
                    database.reminderDao().updateReminder(
                        it.copy(updatedAt = System.currentTimeMillis())
                    )
                }
            }

            // ✅ Create next occurrence for recurring reminders (only when auto-dismissed)
            if (!isManual) {
                val reminderDao = database.reminderDao()
                val reminder = reminderDao.getReminderByIdSync(currentReminderId)

                if (reminder != null &&
                    reminder.recurrenceType != "ONE_TIME" &&
                    reminder.recurringGroupId != null) {

                    val baseTime = maxOf(reminder.dateTime, System.currentTimeMillis())
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = baseTime

                    when (reminder.recurrenceType) {
                        "HOURLY" -> calendar.add(java.util.Calendar.HOUR_OF_DAY, reminder.recurrenceInterval)
                        "DAILY" -> calendar.add(java.util.Calendar.DAY_OF_YEAR, reminder.recurrenceInterval)
                        "WEEKLY" -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, reminder.recurrenceInterval)
                        "MONTHLY" -> calendar.add(java.util.Calendar.MONTH, reminder.recurrenceInterval)
                        "ANNUAL" -> calendar.add(java.util.Calendar.YEAR, reminder.recurrenceInterval)
                    }

                    val nextReminder = reminder.copy(
                        id = 0,
                        dateTime = calendar.timeInMillis,
                        isCompleted = false,
                        isDeleted = false,
                        completedAt = null,
                        dismissalReason = null,
                        snoozeCount = 0,
                        updatedAt = System.currentTimeMillis()
                    )

                    val newId = reminderDao.insertReminder(nextReminder)
                    val alarmScheduler = AlarmScheduler(applicationContext)
                    alarmScheduler.scheduleAlarm(nextReminder.copy(id = newId))
                }
            }
        }

        stopAlarmSound()
        stopSelf()
    }
    private fun createNotification(
        title: String,
        notes: String,
        reminderId: Long,
        snoozeCount: Int = 0
    ): android.app.Notification {
        // Show "Second Reminder" / "Third Reminder" prefix
        val ringLabel = when (snoozeCount) {
            1 -> "Second Reminder: "
            2 -> "Third Reminder: "
            else -> ""
        }
        val displayTitle = "$ringLabel$title"

        // Snooze action
        val snoozeIntent = Intent(this, AlarmSnoozeReceiver::class.java).apply {
            action = "com.example.myreminders_claude2.ACTION_SNOOZE"
            putExtra("REMINDER_ID", reminderId)
            putExtra("TITLE", title)
            putExtra("NOTES", notes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.toSafeInt(),  // ✅ P1 #5: safe Long→Int
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss action
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            action = "com.example.myreminders_claude2.ACTION_DISMISS"
            putExtra("REMINDER_ID", reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.toSafeInt() + 1000000,  // ✅ P1 #5
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open app action (for tapping the notification body)
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("REMINDER_ID", reminderId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            reminderId.toSafeInt() + 2000000,  // ✅ P1 #5
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(displayTitle)
            .setContentText(notes.ifBlank { "Use buttons below to snooze or dismiss" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())

        // Add actions - these will appear as buttons
        builder.addAction(
            android.R.drawable.ic_lock_idle_alarm,
            "Snooze",
            snoozePendingIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Dismiss",
            dismissPendingIntent
        )

        // Use BigTextStyle to ensure notification expands and shows buttons
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(notes.ifBlank { "Use buttons below to snooze or dismiss" })
                .setBigContentTitle(displayTitle)
        )

        return builder.build()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminder Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for reminder alarms"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }

            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun playAlarmSound() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 500, 500, 500, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS && isVoiceEnabled) {
            textToSpeech?.language = Locale.getDefault()
            handler.postDelayed({
                textToSpeech?.speak(reminderText, TextToSpeech.QUEUE_ADD, null, "reminder")
            }, 1000)
        }
    }

    private fun stopAlarmSound() {
        Log.d("AlarmService", "stopAlarmSound() called")
        handler.removeCallbacksAndMessages(null)

        mediaPlayer?.apply {
            if (isPlaying) {
                Log.d("AlarmService", "Stopping media player")
                stop()
            }
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        Log.d("AlarmService", "Vibration cancelled")

        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null

        Log.d("AlarmService", "stopAlarmSound() complete - NOT calling stopForeground here")
        // Don't call stopForeground here - let the action handler do it
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }
}