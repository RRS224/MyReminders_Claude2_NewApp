package com.example.myreminders_claude2.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myreminders_claude2.ui.theme.MyRemindersTheme
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnoozeDurationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("SnoozeDuration", "=== onCreate called ===")

        // Show over lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val reminderId = intent.getLongExtra("REMINDER_ID", -1L)
        val title = intent.getStringExtra("TITLE") ?: ""
        val notes = intent.getStringExtra("NOTES") ?: ""

        setContent {
            MyRemindersTheme {
                SnoozeDurationDialog(
                    onDurationSelected = { minutes ->
                        performSnooze(reminderId, title, notes, minutes)
                        Log.d("SnoozeDuration", "Finishing activity")
                        finish()
                    },
                    onDismiss = {
                        Log.d("SnoozeDuration", "Dialog dismissed, finishing activity")
                        finish()
                    }
                )
            }
        }
    }

    private fun performSnooze(reminderId: Long, title: String, notes: String, minutes: Int) {
        Log.d("SnoozeDuration", "Performing snooze for $minutes minutes")

        // Stop current alarm
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
        }
        startService(stopIntent)
        Log.d("SnoozeDuration", "Stop intent sent")

        // Schedule new alarm
        val alarmScheduler = AlarmScheduler(this)
        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        alarmScheduler.scheduleAlarmForSnooze(
            reminderId = reminderId,
            triggerTime = snoozeTime,
            title = title,
            notes = notes,
            isVoiceEnabled = true
        )

        // Update database
        CoroutineScope(Dispatchers.IO).launch {
            val database = com.example.myreminders_claude2.data.ReminderDatabase.getDatabase(applicationContext)
            val reminder = database.reminderDao().getReminderByIdSync(reminderId)
            reminder?.let {
                val updated = it.copy(
                    dateTime = snoozeTime,
                    // ✅ FIX: Increment snoozeCount so "Second/Third Reminder" labels
                    // and the auto-snooze limiter both reflect manual snoozes correctly
                    snoozeCount = it.snoozeCount + 1,
                    // ✅ Bump updatedAt so Firebase sync picks up this change
                    updatedAt = System.currentTimeMillis()
                )
                database.reminderDao().updateReminder(updated)
            }
        }
    }
}

@Composable
fun SnoozeDurationDialog(
    onDurationSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Snooze for...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (showCustomInput) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = { showCustomInput = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val mins = customMinutes.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    onDurationSelected(mins)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = customMinutes.toIntOrNull()?.let { it > 0 } == true
        ) {
                            Text("Snooze")
                        }
                    }
                } else {
                    SnoozeButton("5 minutes", 5, onDurationSelected)
                    SnoozeButton("10 minutes", 10, onDurationSelected)
                    SnoozeButton("15 minutes", 15, onDurationSelected)
                    SnoozeButton("30 minutes", 30, onDurationSelected)
                    SnoozeButton("1 hour", 60, onDurationSelected)

                    OutlinedButton(
                        onClick = { showCustomInput = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Custom...")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun SnoozeButton(label: String, minutes: Int, onClick: (Int) -> Unit) {
    Button(
        onClick = { onClick(minutes) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}
