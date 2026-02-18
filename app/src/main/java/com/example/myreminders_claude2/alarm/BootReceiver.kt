package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myreminders_claude2.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received action: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d("BootReceiver", "Rescheduling all alarms due to: ${intent.action}")
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
}