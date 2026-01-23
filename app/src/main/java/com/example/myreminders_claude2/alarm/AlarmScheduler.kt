package com.example.myreminders_claude2.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.myreminders_claude2.MainActivity
import com.example.myreminders_claude2.data.Reminder
import java.text.SimpleDateFormat
import java.util.*

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val TAG = "AlarmScheduler"

    fun scheduleAlarm(reminder: Reminder) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
        Log.d(TAG, "=== SCHEDULING ALARM ===")
        Log.d(TAG, "Reminder ID: ${reminder.id}")
        Log.d(TAG, "Title: ${reminder.title}")
        Log.d(TAG, "Scheduled for: ${dateFormat.format(Date(reminder.dateTime))}")
        Log.d(TAG, "Current time: ${dateFormat.format(Date())}")
        Log.d(TAG, "Seconds until alarm: ${(reminder.dateTime - System.currentTimeMillis()) / 1000}")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_NOTES", reminder.notes)
            putExtra("VOICE_ENABLED", reminder.isVoiceEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create a PendingIntent to show alarm info
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("REMINDER_ID", reminder.id)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setAlarmClock for maximum reliability
        // This is the most aggressive and reliable alarm type
        Log.d(TAG, "Using setAlarmClock (MAXIMUM RELIABILITY - will show alarm icon)")

        val alarmClockInfo = AlarmManager.AlarmClockInfo(
            reminder.dateTime,
            showPendingIntent
        )

        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

        Log.d(TAG, "=== ALARM SCHEDULED SUCCESSFULLY (ALARM CLOCK MODE) ===")
        Log.d(TAG, "Note: You'll see an alarm icon in the status bar")
    }

    fun cancelAlarm(reminderId: Long) {
        Log.d(TAG, "Cancelling alarm for reminder ID: $reminderId")
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled successfully")
    }
}