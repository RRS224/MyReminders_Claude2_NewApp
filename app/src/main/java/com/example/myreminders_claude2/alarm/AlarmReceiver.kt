package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
        Log.d("AlarmReceiver", "=== ALARM FIRED ===")
        Log.d("AlarmReceiver", "Actual fire time: ${timeFormat.format(Date())}")

        // ✅ FIX: Removed ACTION_BOOT_COMPLETED handling from here.
        // BootReceiver is the single dedicated handler for boot rescheduling.
        // Having both AlarmReceiver AND BootReceiver handle BOOT_COMPLETED
        // caused every alarm to be scheduled twice after a phone restart.

        val reminderId = intent.getLongExtra("REMINDER_ID", -1)
        val title = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val notes = intent.getStringExtra("REMINDER_NOTES") ?: ""
        val voiceEnabled = intent.getBooleanExtra("VOICE_ENABLED", true)
        val snoozeCount = intent.getIntExtra("SNOOZE_COUNT", 0)

        Log.d("AlarmReceiver", "Starting alarm service for reminder: $title (ID: $reminderId)")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("REMINDER_ID", reminderId)
            putExtra("REMINDER_TITLE", title)
            putExtra("REMINDER_NOTES", notes)
            putExtra("VOICE_ENABLED", voiceEnabled)
            putExtra("SNOOZE_COUNT", snoozeCount)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
