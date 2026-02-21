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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Boot completed - rescheduling alarms")

        val pendingResult = goAsync()
        val database = ReminderDatabase.getDatabase(context)
        val alarmScheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // One-shot fetch - safe for broadcast context
                val reminders = database.reminderDao().getAllActiveRemindersSync()
                Log.d("BootReceiver", "Rescheduling ${reminders.size} reminders after boot")

                val now = System.currentTimeMillis()
                reminders.forEach { reminder ->
                    if (reminder.dateTime > now) {
                        alarmScheduler.scheduleAlarm(reminder)
                        Log.d("BootReceiver", "Rescheduled: ${reminder.title}")
                    }
                }
                Log.d("BootReceiver", "Boot reschedule complete")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error rescheduling alarms after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
