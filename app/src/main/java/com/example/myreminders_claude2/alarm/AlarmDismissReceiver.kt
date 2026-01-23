package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myreminders_claude2.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("REMINDER_ID", -1)

        if (reminderId != -1L) {
            // Mark as completed in database
            CoroutineScope(Dispatchers.IO).launch {
                val database = ReminderDatabase.getDatabase(context)
                database.reminderDao().markAsCompleted(
                    reminderId,
                    true,
                    System.currentTimeMillis(),
                    "MANUAL"
                )
            }

            // Stop the alarm service
            val serviceIntent = Intent(context, AlarmService::class.java)
            context.stopService(serviceIntent)
        }
    }
}