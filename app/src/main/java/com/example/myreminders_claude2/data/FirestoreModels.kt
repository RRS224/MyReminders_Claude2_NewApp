package com.example.myreminders_claude2.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.*

data class FirestoreReminder(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val dateTime: Timestamp? = null,
    val category: String = "Personal",
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val userId: String = ""
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