package com.example.myreminders_claude2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "permanently_deleted")
data class PermanentlyDeleted(
    @PrimaryKey val reminderId: Long
)