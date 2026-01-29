package com.example.myreminders_claude2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class Template(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Template identification
    val name: String,                    // "Weekly Team Meeting"

    // Reminder content
    val title: String,                   // "Team sync - discuss projects"
    val notes: String = "",              // "Bring status updates"

    // Categorization
    val mainCategory: String,            // "WORK"
    val subCategory: String? = null,     // "Meeting"

    // Recurrence settings
    val recurrenceType: String,          // "WEEKLY", "DAILY", etc.
    val recurrenceInterval: Int = 1,     // Every X days/weeks/months

    // Voice settings
    val isVoiceEnabled: Boolean = true,

    // Analytics
    val usageCount: Int = 0,             // Track how often template is used
    val lastUsedAt: Long? = null,        // When was it last used

    // Metadata
    val createdAt: Long = System.currentTimeMillis()
)