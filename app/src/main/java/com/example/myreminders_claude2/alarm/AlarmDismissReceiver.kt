package com.example.myreminders_claude2.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myreminders_claude2.data.RecurrenceType
import com.example.myreminders_claude2.data.ReminderDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("REMINDER_ID", -1)

        if (reminderId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                val database = ReminderDatabase.getDatabase(context)
                val reminderDao = database.reminderDao()

                // Get the reminder BEFORE marking as completed
                val reminder = reminderDao.getReminderByIdSync(reminderId)

                // ✅ Dismissed reminders go to Completed tab
                reminderDao.softDeleteReminder(reminderId, System.currentTimeMillis())

                // ✅ Create next occurrence for recurring reminders
                if (reminder != null &&
                    reminder.recurrenceType != RecurrenceType.ONE_TIME &&
                    reminder.recurringGroupId != null) {

                    // ✅ Check if a future occurrence already exists - prevents duplicates!
                    val existingFuture = reminderDao.getFutureRemindersInGroup(
                        reminder.recurringGroupId,
                        System.currentTimeMillis()
                    )

                    if (existingFuture.isEmpty()) {
                        // Only create if no future reminder exists yet!
                        val baseTime = maxOf(reminder.dateTime, System.currentTimeMillis())

                        val nextDateTime = calculateNextOccurrence(
                            currentDateTime = baseTime,
                            recurrenceType = reminder.recurrenceType,
                            recurrenceInterval = reminder.recurrenceInterval,
                            recurrenceDayOfWeek = reminder.recurrenceDayOfWeek,
                            recurrenceDayOfMonth = reminder.recurrenceDayOfMonth
                        )

                        val nextReminder = reminder.copy(
                            id = 0,
                            dateTime = nextDateTime,
                            isCompleted = false,
                            isDeleted = false,
                            completedAt = null,
                            dismissalReason = null,
                            snoozeCount = 0,
                            updatedAt = System.currentTimeMillis()
                        )

                        val newId = reminderDao.insertReminder(nextReminder)
                        val alarmScheduler = AlarmScheduler(context)
                        alarmScheduler.scheduleAlarm(nextReminder.copy(id = newId))
                        Log.d("AlarmDismissReceiver", "Created next occurrence with id: $newId")
                    } else {
                        Log.d("AlarmDismissReceiver", "Next occurrence already exists - skipping creation")
                    }
                }
            }

            // Stop the alarm service
            val serviceIntent = Intent(context, AlarmService::class.java)
            context.stopService(serviceIntent)
        }
    }

    private fun calculateNextOccurrence(
        currentDateTime: Long,
        recurrenceType: String,
        recurrenceInterval: Int,
        recurrenceDayOfWeek: Int?,
        recurrenceDayOfMonth: Int?
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDateTime

        when (recurrenceType) {
            RecurrenceType.HOURLY -> {
                calendar.add(Calendar.HOUR_OF_DAY, recurrenceInterval)
            }
            RecurrenceType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, recurrenceInterval)
            }
            RecurrenceType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, recurrenceInterval)
            }
            RecurrenceType.MONTHLY -> {
                calendar.add(Calendar.MONTH, recurrenceInterval)
                if (recurrenceDayOfMonth != null) {
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val targetDay = minOf(recurrenceDayOfMonth, maxDay)
                    calendar.set(Calendar.DAY_OF_MONTH, targetDay)
                }
            }
            RecurrenceType.ANNUAL -> {
                calendar.add(Calendar.YEAR, recurrenceInterval)
            }
        }

        return calendar.timeInMillis
    }
}