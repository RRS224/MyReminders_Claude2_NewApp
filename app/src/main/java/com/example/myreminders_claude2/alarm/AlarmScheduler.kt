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

    // ✅ P1 #5: Safe Long→Int for PendingIntent request codes — avoids collision from truncation
    private fun Long.toRequestCode(): Int = (this and 0x7FFFFFFF).toInt()

    fun scheduleAlarm(reminder: Reminder) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
        Log.d(TAG, "=== SCHEDULING ALARM ===")
        Log.d(TAG, "Reminder ID: ${reminder.id}")
        Log.d(TAG, "Title: ${reminder.title}")
        Log.d(TAG, "Scheduled for: ${dateFormat.format(Date(reminder.dateTime))}")
        Log.d(TAG, "Current time: ${dateFormat.format(Date())}")
        Log.d(
            TAG,
            "Seconds until alarm: ${(reminder.dateTime - System.currentTimeMillis()) / 1000}"
        )

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_NOTES", reminder.notes)
            putExtra("VOICE_ENABLED", reminder.isVoiceEnabled)
            putExtra("SNOOZE_COUNT", reminder.snoozeCount) // ✅ Pass snooze count
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toRequestCode(),
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
            reminder.id.toRequestCode(),
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
            reminderId.toRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled successfully")
    }

    // Overload for snooze - schedules without full Reminder object
    fun scheduleAlarmForSnooze(
        reminderId: Long,
        triggerTime: Long,
        title: String,
        notes: String,
        isVoiceEnabled: Boolean = true
    ) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
        Log.d(TAG, "=== SCHEDULING SNOOZE ALARM ===")
        Log.d(TAG, "Reminder ID: $reminderId")
        Log.d(TAG, "Title: $title")
        Log.d(TAG, "Scheduled for: ${dateFormat.format(Date(triggerTime))}")

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminderId)
            putExtra("REMINDER_TITLE", title)
            putExtra("REMINDER_NOTES", notes)
            putExtra("VOICE_ENABLED", isVoiceEnabled)
            putExtra("SNOOZE_COUNT", 0) // Reset snooze count for manual snooze
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toRequestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("REMINDER_ID", reminderId)
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toRequestCode(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, showPendingIntent)
        alarmManager.setAlarmClock(alarmInfo, pendingIntent)
        Log.d(TAG, "Snooze alarm set successfully")
    }
}