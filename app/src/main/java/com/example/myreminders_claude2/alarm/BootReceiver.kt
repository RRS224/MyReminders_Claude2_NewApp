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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // ✅ FIX: goAsync() prevents Android from killing the receiver before
            // the coroutine finishes re-scheduling all alarms after a phone restart.
            val pendingResult = goAsync()

            Log.d("BootReceiver", "Boot completed - re-scheduling all active alarms")

            val database = ReminderDatabase.getDatabase(context)
            val alarmScheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // One-time sync fetch (not a Flow) so the receiver exits cleanly
                    val reminders = database.reminderDao().getAllActiveRemindersSync()
                    Log.d("BootReceiver", "Re-scheduling ${reminders.size} active reminders")

                    reminders.forEach { reminder ->
                        if (reminder.dateTime > System.currentTimeMillis()) {
                            alarmScheduler.scheduleAlarm(reminder)
                        }
                    }

                    Log.d("BootReceiver", "All alarms re-scheduled successfully")
                } finally {
                    // ✅ Always release so Android knows the receiver is done
                    pendingResult.finish()
                }
            }
        }
    }
}
