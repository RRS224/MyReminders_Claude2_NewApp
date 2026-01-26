package com.example.myreminders_claude2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // Get all active reminders (not completed)
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dateTime ASC")
    fun getAllActiveReminders(): Flow<List<Reminder>>

    // Get all completed reminders
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    // Get reminder by ID
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): Reminder?

    // Insert reminder
    @Insert
    suspend fun insertReminder(reminder: Reminder): Long

    // Update reminder
    @Update
    suspend fun updateReminder(reminder: Reminder)

    // Delete reminder
    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    // Delete reminder by ID
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    // Mark reminder as completed
    @Query("UPDATE reminders SET isCompleted = :isCompleted, completedAt = :completedAt, dismissalReason = :reason WHERE id = :reminderId")
    suspend fun markAsCompleted(reminderId: Long, isCompleted: Boolean, completedAt: Long, reason: String)

    // Update snooze count
    @Query("UPDATE reminders SET snoozeCount = :count WHERE id = :reminderId")
    suspend fun updateSnoozeCount(reminderId: Long, count: Int)

    // Clear all completed reminders
    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun clearAllCompleted()

    // NEW: Get all reminders in a recurring group
    @Query("SELECT * FROM reminders WHERE recurringGroupId = :groupId ORDER BY dateTime ASC")
    fun getRemindersInRecurringGroup(groupId: String): Flow<List<Reminder>>

    // NEW: Get all future reminders in a recurring group
    @Query("SELECT * FROM reminders WHERE recurringGroupId = :groupId AND dateTime >= :currentTime ORDER BY dateTime ASC")
    suspend fun getFutureRemindersInGroup(groupId: String, currentTime: Long): List<Reminder>

    // NEW: Delete all future reminders in a recurring group
    @Query("DELETE FROM reminders WHERE recurringGroupId = :groupId AND dateTime >= :currentTime AND id != :excludeId")
    suspend fun deleteFutureRemindersInGroup(groupId: String, currentTime: Long, excludeId: Long)

    // NEW: Update all future reminders in a recurring group (for editing)
    @Query("UPDATE reminders SET title = :title, notes = :notes, mainCategory = :mainCategory, subCategory = :subCategory WHERE recurringGroupId = :groupId AND dateTime >= :currentTime")
    suspend fun updateFutureRemindersInGroup(groupId: String, currentTime: Long, title: String, notes: String, mainCategory: String, subCategory: String?)

    // NEW: Get active reminders by recurrence type
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND recurrenceType = :type ORDER BY dateTime ASC")
    fun getActiveRemindersByRecurrenceType(type: String): Flow<List<Reminder>>

    // NEW: Get active reminders by main category
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND mainCategory = :category ORDER BY dateTime ASC")
    fun getActiveRemindersByCategory(category: String): Flow<List<Reminder>>

    // NEW: Get completed reminders by recurrence type
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND recurrenceType = :type ORDER BY completedAt DESC")
    fun getCompletedRemindersByRecurrenceType(type: String): Flow<List<Reminder>>

    // NEW: Get completed reminders by main category
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND mainCategory = :category ORDER BY completedAt DESC")
    fun getCompletedRemindersByCategory(category: String): Flow<List<Reminder>>

    // NEW: Check if a recurring group has any active instances
    @Query("SELECT COUNT(*) FROM reminders WHERE recurringGroupId = :groupId AND isCompleted = 0")
    suspend fun getActiveCountInRecurringGroup(groupId: String): Int

    // ===== CATEGORY MANAGEMENT METHODS =====

    @Query("SELECT COUNT(*) FROM reminders WHERE mainCategory = :category AND isCompleted = 0")
    suspend fun getRemindersCountByCategory(category: String): Int

    @Query("UPDATE reminders SET mainCategory = :newCategory WHERE mainCategory = :oldCategory")
    suspend fun updateCategoryForAllReminders(oldCategory: String, newCategory: String)
}