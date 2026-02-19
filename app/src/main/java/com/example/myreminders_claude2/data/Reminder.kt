package com.example.myreminders_claude2.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ✅ FIX: Added indices on the columns most frequently used in DAO queries.
// Without indices, every query does a full table scan. As the reminder count
// grows (hundreds of completed/deleted reminders), queries slow down noticeably.
//
// Index breakdown:
//   idx_active      — powers getAllActiveReminders() (isDeleted=0, isCompleted=0, ORDER BY dateTime)
//   idx_completed   — powers getCompletedReminders() (isCompleted=1, isDeleted=0, ORDER BY completedAt)
//   idx_deleted     — powers getDeletedReminders() (isDeleted=1, ORDER BY deletedAt)
//   idx_group       — powers getFutureRemindersInGroup() and all recurringGroupId lookups
//   idx_category    — powers getActiveRemindersByCategory() and getRemindersCountByCategory()
@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["isDeleted", "isCompleted", "dateTime"],  name = "idx_active"),
        Index(value = ["isCompleted", "isDeleted", "completedAt"], name = "idx_completed"),
        Index(value = ["isDeleted", "deletedAt"],                name = "idx_deleted"),
        Index(value = ["recurringGroupId", "dateTime"],          name = "idx_group"),
        Index(value = ["mainCategory"],                          name = "idx_category")
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Basic fields
    val title: String,
    val notes: String = "",
    val dateTime: Long,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val dismissalReason: String? = null,
    val snoozeCount: Int = 0,

    // Voice TTS field
    val isVoiceEnabled: Boolean = true, // Read aloud when alarm fires (default: ON)

    // Recurrence fields
    val recurrenceType: String = RecurrenceType.ONE_TIME, // ONE_TIME, HOURLY, DAILY, WEEKLY, MONTHLY, ANNUAL
    val recurrenceInterval: Int = 1, // Every X hours/days/weeks/months
    val recurrenceDayOfWeek: Int? = null, // For WEEKLY: 1=Sun, 2=Mon, 3=Tue, etc.
    val recurrenceDayOfMonth: Int? = null, // For MONTHLY: 1-31
    val recurringGroupId: String? = null, // Groups all instances of same recurring reminder

    // Category fields
    val mainCategory: String = CategoryDefaults.PERSONAL, // WORK, PERSONAL, HEALTH, FINANCE, or custom
    val subCategory: String? = null, // Call, Meeting, Email, etc.

    // Deletion tracking
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Recurrence type constants
object RecurrenceType {
    const val ONE_TIME = "ONE_TIME"
    const val HOURLY   = "HOURLY"
    const val DAILY    = "DAILY"
    const val WEEKLY   = "WEEKLY"
    const val MONTHLY  = "MONTHLY"
    const val ANNUAL   = "ANNUAL"
}

// Category constants
object CategoryDefaults {
    const val WORK     = "WORK"
    const val PERSONAL = "PERSONAL"
    const val HEALTH   = "HEALTH"
    const val FINANCE  = "FINANCE"
}
