package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.myreminders_claude2.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
        Log.d("AlarmReceiver", "=== ALARM FIRED ===")
        Log.d("AlarmReceiver", "Actual fire time: ${timeFormat.format(Date())}")
        Log.d("AlarmReceiver", "Action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d("AlarmReceiver", "Boot completed - re-scheduling alarms")
                rescheduleAllAlarms(context)
            }
            else -> {
                val reminderId = intent.getLongExtra("REMINDER_ID", -1)
                val title = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
                val notes = intent.getStringExtra("REMINDER_NOTES") ?: ""
                val voiceEnabled = intent.getBooleanExtra("VOICE_ENABLED", true)

                Log.d("AlarmReceiver", "Starting alarm service for reminder: $title (ID: $reminderId)")

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("REMINDER_ID", reminderId)
                    putExtra("REMINDER_TITLE", title)
                    putExtra("REMINDER_NOTES", notes)
                    putExtra("VOICE_ENABLED", voiceEnabled)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        val database = ReminderDatabase.getDatabase(context)
        val alarmScheduler = AlarmScheduler(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = database.reminderDao().getAllActiveReminders().first()
                Log.d("AlarmReceiver", "Re-scheduling ${reminders.size} active reminders after boot")

                reminders.forEach { reminder ->
                    if (reminder.dateTime > System.currentTimeMillis()) {
                        alarmScheduler.scheduleAlarm(reminder)
                    }
                }

                Log.d("AlarmReceiver", "All alarms re-scheduled successfully")
            } finally {
                pendingResult.finish()
            }
        }
    }
}