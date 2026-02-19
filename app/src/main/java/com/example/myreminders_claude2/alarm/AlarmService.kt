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
import com.example.myreminders_claude2.data.RecurrenceType
import com.example.myreminders_claude2.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    // ✅ FIX: Single managed scope - cancelled in onDestroy to prevent leaks
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

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
                stopAlarmSound()
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

                val notification = createNotification(title, notes, currentReminderId, currentSnoozeCount)
                startForeground(NOTIFICATION_ID, notification)

                playAlarmSound()
                startVibration()
                scheduleAutoSnooze()

                // ✅ FIX: START_REDELIVER_INTENT ensures alarm restarts if process is killed mid-ring
                START_REDELIVER_INTENT
            }
        }
    }

    private fun scheduleAutoSnooze() {
        // ✅ FIX: Capture ID as immutable local before postDelayed.
        // Without this, if a second alarm fires before the 30s timer triggers,
        // the runnable would act on the new alarm's ID instead of this one.
        val capturedId = currentReminderId
        handler.postDelayed({
            serviceScope.launch {
                val database = ReminderDatabase.getDatabase(applicationContext)
                val reminder = database.reminderDao().getReminderById(capturedId)

                reminder?.let {
                    if (it.snoozeCount < MAX_AUTO_SNOOZES) {
                        performAutoSnooze(capturedId)
                    } else {
                        handleDismiss(isManual = false, reminderId = capturedId)
                    }
                }
            }
        }, RING_DURATION)
    }

    private fun performAutoSnooze(reminderId: Long = currentReminderId) {
        stopAlarmSound()

        serviceScope.launch {
            val database = ReminderDatabase.getDatabase(applicationContext)
            val reminder = database.reminderDao().getReminderById(reminderId)

            reminder?.let {
                val newSnoozeCount: Int = it.snoozeCount + 1
                val newDateTime: Long = System.currentTimeMillis() + SNOOZE_INTERVAL

                database.reminderDao().updateSnoozeCount(reminderId, newSnoozeCount)

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

    private fun handleDismiss(isManual: Boolean, reminderId: Long = currentReminderId) {
        serviceScope.launch {
            val database = ReminderDatabase.getDatabase(applicationContext)
            val reminderDao = database.reminderDao()
            val completedAt: Long = System.currentTimeMillis()
            val dismissalReason: String = if (isManual) "MANUAL" else "AUTO_SNOOZED"

            if (isManual) {
                // Manually dismissed → goes to Done tab
                reminderDao.softDeleteReminder(reminderId, completedAt)
            } else {
                // Unattended (rang 3 times, ignored) → goes to Missed tab
                reminderDao.markAsCompleted(
                    reminderId,
                    true,
                    completedAt,
                    dismissalReason
                )
                // Update updatedAt so Firebase sync doesn't revert it
                val reminder = reminderDao.getReminderByIdSync(reminderId)
                reminder?.let {
                    reminderDao.updateReminder(
                        it.copy(updatedAt = System.currentTimeMillis())
                    )
                }
            }

            // ✅ FIX: Create next occurrence for recurring reminders on BOTH manual and auto dismiss.
            // Previously only ran for auto-dismiss, so manually dismissed recurring reminders
            // would silently break the series.
            val reminderForRecurrence = reminderDao.getReminderByIdSync(reminderId)
            if (reminderForRecurrence != null &&
                reminderForRecurrence.recurrenceType != RecurrenceType.ONE_TIME &&
                reminderForRecurrence.recurringGroupId != null) {

                // Guard against duplicates in case both service and receiver paths run
                val existingFuture = reminderDao.getFutureRemindersInGroup(
                    reminderForRecurrence.recurringGroupId,
                    System.currentTimeMillis()
                )

                if (existingFuture.isEmpty()) {
                    val baseTime = maxOf(reminderForRecurrence.dateTime, System.currentTimeMillis())
                    val calendar = java.util.Calendar.getInstance()
                    calendar.timeInMillis = baseTime

                    when (reminderForRecurrence.recurrenceType) {
                        RecurrenceType.HOURLY  -> calendar.add(java.util.Calendar.HOUR_OF_DAY, reminderForRecurrence.recurrenceInterval)
                        RecurrenceType.DAILY   -> calendar.add(java.util.Calendar.DAY_OF_YEAR,  reminderForRecurrence.recurrenceInterval)
                        RecurrenceType.WEEKLY  -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, reminderForRecurrence.recurrenceInterval)
                        RecurrenceType.MONTHLY -> calendar.add(java.util.Calendar.MONTH,        reminderForRecurrence.recurrenceInterval)
                        RecurrenceType.ANNUAL  -> calendar.add(java.util.Calendar.YEAR,         reminderForRecurrence.recurrenceInterval)
                    }

                    val nextReminder = reminderForRecurrence.copy(
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
                    Log.d("AlarmService", "Created next recurring occurrence with id: $newId")
                } else {
                    Log.d("AlarmService", "Next recurring occurrence already exists - skipping")
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
            reminderId.toInt(),
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
            reminderId.toInt() + 1000000,
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
            reminderId.toInt() + 2000000,
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

        builder.addAction(
            android.R.drawable.ic_lock_idle_alarm,
            "Snooze 10m",
            snoozePendingIntent
        )
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Dismiss",
            dismissPendingIntent
        )

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

        Log.d("AlarmService", "stopAlarmSound() complete")
    }

    override fun onDestroy() {
        stopAlarmSound()
        // ✅ FIX: Cancel all coroutines when service is destroyed
        serviceJob.cancel()
        super.onDestroy()
    }
}
