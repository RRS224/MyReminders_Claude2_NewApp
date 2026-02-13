package com.example.myreminders_claude2.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class FirestoreReminder(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val category: String = "Personal",
    val completed: Boolean = false,
    val deleted: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val userId: String = "",
    // ✅ Added missing fields:
    val completedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null,
    val dismissalReason: String? = null,
    val recurrenceType: String = "ONE_TIME",
    val recurrenceInterval: Int = 1,
    val recurrenceDayOfWeek: Int? = null,
    val recurrenceDayOfMonth: Int? = null,
    val recurringGroupId: String? = null,
    val subCategory: String? = null,
    val isVoiceEnabled: Boolean = true,
    val snoozeCount: Int = 0
)

data class FirestoreCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val icon: String = "",
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class FirestoreTemplate(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "Personal",
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now()
)