package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myreminders_claude2.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule all active reminders after boot
            val database = ReminderDatabase.getDatabase(context)
            val alarmScheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                // Get all active (non-completed) reminders - one-time fetch
                val reminders = database.reminderDao().getAllActiveRemindersSync()

                // Re-schedule each reminder
                reminders.forEach { reminder ->
                    // Only re-schedule if the time is in the future
                    if (reminder.dateTime > System.currentTimeMillis()) {
                        alarmScheduler.scheduleAlarm(reminder)
                    }
                }
            }
        }
    }
}