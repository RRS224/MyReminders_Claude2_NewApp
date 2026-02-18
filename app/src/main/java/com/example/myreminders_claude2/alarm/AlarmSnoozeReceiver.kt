package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.myreminders_claude2.data.ReminderDatabase

class AlarmSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.widget.Toast.makeText(context, "SNOOZE RECEIVER TRIGGERED!", android.widget.Toast.LENGTH_LONG).show()
        Log.d("AlarmSnoozeReceiver", "=== SNOOZE BUTTON TAPPED ===")
        val reminderId = intent.getLongExtra("REMINDER_ID", -1L)
        Log.d("AlarmSnoozeReceiver", "Reminder ID: $reminderId")
        val title = intent.getStringExtra("TITLE") ?: ""
        val notes = intent.getStringExtra("NOTES") ?: ""

        if (reminderId == -1L) return

        Log.d("AlarmSnoozeReceiver", "Snoozing reminder $reminderId for 10 minutes")

        // Stop current alarm
        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        context.startService(stopIntent)

        // Schedule new alarm 10 minutes from now
        val alarmScheduler = AlarmScheduler(context)
        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes

        // Update the reminder's dateTime in the database
        CoroutineScope(Dispatchers.IO).launch {
            val database = ReminderDatabase.getDatabase(context)
            val reminder = database.reminderDao().getReminderByIdSync(reminderId)
            reminder?.let {
                val updated = it.copy(dateTime = snoozeTime)
                database.reminderDao().updateReminder(updated)
            }
        }

        alarmScheduler.scheduleAlarmForSnooze(
            reminderId = reminderId,
            triggerTime = snoozeTime,
            title = title,
            notes = notes,
            isVoiceEnabled = true
        )
    }
}