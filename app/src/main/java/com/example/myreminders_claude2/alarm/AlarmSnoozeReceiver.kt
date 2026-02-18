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
        Log.d("AlarmSnoozeReceiver", "=== SNOOZE BUTTON TAPPED ===")
        android.widget.Toast.makeText(context, "Opening snooze picker...", android.widget.Toast.LENGTH_SHORT).show()

        // Launch duration picker activity
        val reminderId = intent.getLongExtra("REMINDER_ID", -1L)
        val title = intent.getStringExtra("TITLE") ?: ""
        val notes = intent.getStringExtra("NOTES") ?: ""

        Log.d("AlarmSnoozeReceiver", "Reminder ID: $reminderId, Title: $title")

        val pickerIntent = Intent(context, SnoozeDurationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("REMINDER_ID", reminderId)
            putExtra("TITLE", title)
            putExtra("NOTES", notes)
        }

        try {
            Log.d("AlarmSnoozeReceiver", "Attempting to start SnoozeDurationActivity")
            context.startActivity(pickerIntent)
            Log.d("AlarmSnoozeReceiver", "Activity start call completed")
        } catch (e: Exception) {
            Log.e("AlarmSnoozeReceiver", "Failed to start activity", e)
            android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

