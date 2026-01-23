package com.example.myreminders_claude2.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
    private val handler: Handler = Handler(Looper.getMainLooper())

    companion object {
        const val CHANNEL_ID = "reminder_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_DISMISS = "com.example.myreminders_claude2.ACTION_DISMISS"
        const val ACTION_STOP_ALARM = "com.example.myreminders_claude2.ACTION_STOP_ALARM"
        private const val MAX_AUTO_SNOOZES = 3
        private const val RING_DURATION = 1 * 60 * 1000L
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
                // Dismiss button was tapped - mark as complete and stop everything
                handleDismiss(isManual = true)
                START_NOT_STICKY
            }
            ACTION_STOP_ALARM -> {
                // Notification body was tapped - stop sound and open full screen
                val reminderIdFromIntent: Long = intent.getLongExtra("REMINDER_ID", -1)

                // Stop the alarm sound
                stopAlarmSound()

                // Open MainActivity with the reminder to show full screen
                if (reminderIdFromIntent != -1L) {
                    val openIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("REMINDER_ID", reminderIdFromIntent)
                    }
                    startActivity(openIntent)
                }

                START_NOT_STICKY
            }
            else -> {
                // New alarm triggered
                currentReminderId = intent?.getLongExtra("REMINDER_ID", -1) ?: -1
                val title: String = intent?.getStringExtra("REMINDER_TITLE") ?: "Reminder"
                val notes: String = intent?.getStringExtra("REMINDER_NOTES") ?: ""
                isVoiceEnabled = intent?.getBooleanExtra("VOICE_ENABLED", true) ?: true

                reminderText = if (notes.isNotBlank()) {
                    "$title. $notes"
                } else {
                    title
                }

                val notification = createNotification(title, notes, currentReminderId)
                startForeground(NOTIFICATION_ID, notification)

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

                // Update snooze count
                database.reminderDao().updateSnoozeCount(currentReminderId, newSnoozeCount)

                // Update the reminder's dateTime
                val updated = it.copy(
                    dateTime = newDateTime,
                    snoozeCount = newSnoozeCount
                )
                database.reminderDao().updateReminder(updated)

                // Reschedule alarm
                val alarmScheduler = AlarmScheduler(applicationContext)
                alarmScheduler.scheduleAlarm(updated)
            }
        }
    }

    private fun handleDismiss(isManual: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val database = ReminderDatabase.getDatabase(applicationContext)
            val completedAt: Long = System.currentTimeMillis()
            val dismissalReason: String = if (isManual) "MANUAL" else "AUTO_SNOOZED"
            database.reminderDao().markAsCompleted(
                currentReminderId,
                true,
                completedAt,
                dismissalReason
            )
        }

        stopAlarmSound()
        stopSelf()
    }

    private fun createNotification(title: String, notes: String, reminderId: Long): android.app.Notification {
        // Intent for tapping the notification body - stops alarm and opens full screen
        val stopAndOpenIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP_ALARM
            putExtra("REMINDER_ID", reminderId)
        }

        val openPendingIntent: PendingIntent = PendingIntent.getService(
            this,
            reminderId.toInt(),
            stopAndOpenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for dismiss button - broadcasts to AlarmDismissReceiver
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminderId)
        }

        val dismissPendingIntent: PendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.toInt() + 10000, // Different request code
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(notes.ifBlank { "Tap to view" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openPendingIntent) // Tapping notification stops sound & opens app
            .setAutoCancel(false)
            .setOngoing(false)

        // Add dismiss action button
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Dismiss",
            dismissPendingIntent // Tapping button dismisses
        )

        if (notes.isNotBlank()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notes)
                    .setBigContentTitle(title)
            )
        }

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
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
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
        handler.removeCallbacksAndMessages(null)

        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()

        textToSpeech?.apply {
            stop()
            shutdown()
        }
        textToSpeech = null

        stopForeground(STOP_FOREGROUND_DETACH)
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }
}