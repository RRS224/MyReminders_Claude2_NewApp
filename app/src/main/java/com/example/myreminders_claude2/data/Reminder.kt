package com.example.myreminders_claude2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
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

    val createdAt: Long = System.currentTimeMillis()
)

// Recurrence type constants
object RecurrenceType {
    const val ONE_TIME = "ONE_TIME"
    const val HOURLY = "HOURLY"
    const val DAILY = "DAILY"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val ANNUAL = "ANNUAL"
}

// Category constants
object CategoryDefaults {
    const val WORK = "WORK"
    const val PERSONAL = "PERSONAL"
    const val HEALTH = "HEALTH"
    const val FINANCE = "FINANCE"
}