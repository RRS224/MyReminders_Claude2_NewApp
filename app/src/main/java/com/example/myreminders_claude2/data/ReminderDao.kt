package com.example.myreminders_claude2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // Get all active reminders (not completed, not deleted)
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0 ORDER BY dateTime ASC")
    fun getAllActiveReminders(): Flow<List<Reminder>>

    // Get all completed reminders (not deleted)
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isDeleted = 0 ORDER BY completedAt DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    // Get reminder by ID (includes deleted for undelete functionality)
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): Reminder?

    // Insert reminder
    @Insert
    suspend fun insertReminder(reminder: Reminder): Long

    // Update reminder
    @Update
    suspend fun updateReminder(reminder: Reminder)

    // Delete reminder (HARD DELETE - only used internally now)
    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    // Delete reminder by ID (HARD DELETE - only used internally now)
    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    // Mark reminder as completed
    @Query("UPDATE reminders SET isCompleted = :isCompleted, completedAt = :completedAt, dismissalReason = :reason WHERE id = :reminderId")
    suspend fun markAsCompleted(reminderId: Long, isCompleted: Boolean, completedAt: Long, reason: String)

    // Update snooze count
    @Query("UPDATE reminders SET snoozeCount = :count WHERE id = :reminderId")
    suspend fun updateSnoozeCount(reminderId: Long, count: Int)

    // Clear all completed reminders (SOFT DELETE)
    @Query("UPDATE reminders SET isDeleted = 1, deletedAt = :timestamp WHERE isCompleted = 1 AND isDeleted = 0")
    suspend fun clearAllCompleted(timestamp: Long = System.currentTimeMillis())

    // Get all reminders in a recurring group (not deleted)
    @Query("SELECT * FROM reminders WHERE recurringGroupId = :groupId AND isDeleted = 0 ORDER BY dateTime ASC")
    fun getRemindersInRecurringGroup(groupId: String): Flow<List<Reminder>>

    // Get all future reminders in a recurring group (not deleted)
    @Query("SELECT * FROM reminders WHERE recurringGroupId = :groupId AND dateTime >= :currentTime AND isDeleted = 0 ORDER BY dateTime ASC")
    suspend fun getFutureRemindersInGroup(groupId: String, currentTime: Long): List<Reminder>

    // Delete all future reminders in a recurring group (SOFT DELETE)
    @Query("UPDATE reminders SET isDeleted = 1, deletedAt = :timestamp WHERE recurringGroupId = :groupId AND dateTime >= :currentTime AND id != :excludeId AND isDeleted = 0")
    suspend fun deleteFutureRemindersInGroup(groupId: String, currentTime: Long, excludeId: Long, timestamp: Long = System.currentTimeMillis())

    // Update all future reminders in a recurring group (for editing)
    @Query("UPDATE reminders SET title = :title, notes = :notes, mainCategory = :mainCategory, subCategory = :subCategory WHERE recurringGroupId = :groupId AND dateTime >= :currentTime AND isDeleted = 0")
    suspend fun updateFutureRemindersInGroup(groupId: String, currentTime: Long, title: String, notes: String, mainCategory: String, subCategory: String?)

    // Get active reminders by recurrence type (not deleted)
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0 AND recurrenceType = :type ORDER BY dateTime ASC")
    fun getActiveRemindersByRecurrenceType(type: String): Flow<List<Reminder>>

    // Get active reminders by main category (not deleted)
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isDeleted = 0 AND mainCategory = :category ORDER BY dateTime ASC")
    fun getActiveRemindersByCategory(category: String): Flow<List<Reminder>>

    // Get completed reminders by recurrence type (not deleted)
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isDeleted = 0 AND recurrenceType = :type ORDER BY completedAt DESC")
    fun getCompletedRemindersByRecurrenceType(type: String): Flow<List<Reminder>>

    // Get completed reminders by main category (not deleted)
    @Query("SELECT * FROM reminders WHERE isCompleted = 1 AND isDeleted = 0 AND mainCategory = :category ORDER BY completedAt DESC")
    fun getCompletedRemindersByCategory(category: String): Flow<List<Reminder>>

    // Check if a recurring group has any active instances (not deleted)
    @Query("SELECT COUNT(*) FROM reminders WHERE recurringGroupId = :groupId AND isCompleted = 0 AND isDeleted = 0")
    suspend fun getActiveCountInRecurringGroup(groupId: String): Int

    // ===== CATEGORY MANAGEMENT METHODS =====

    // Get reminders count by category (not deleted)
    @Query("SELECT COUNT(*) FROM reminders WHERE mainCategory = :category AND isCompleted = 0 AND isDeleted = 0")
    suspend fun getRemindersCountByCategory(category: String): Int

    // Update category for all reminders (not deleted)
    @Query("UPDATE reminders SET mainCategory = :newCategory WHERE mainCategory = :oldCategory AND isDeleted = 0")
    suspend fun updateCategoryForAllReminders(oldCategory: String, newCategory: String)

    // ===== DELETED REMINDERS QUERIES =====

    /**
     * Get all deleted reminders (max 20, sorted by deletion time)
     */
    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY deletedAt DESC LIMIT 20")
    fun getDeletedReminders(): Flow<List<Reminder>>

    /**
     * Get count of deleted reminders
     */
    @Query("SELECT COUNT(*) FROM reminders WHERE isDeleted = 1")
    suspend fun getDeletedCount(): Int

    /**
     * Soft delete a reminder (mark as deleted instead of removing)
     */
    @Query("UPDATE reminders SET isDeleted = 1, deletedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteReminder(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Soft delete multiple reminders
     */
    @Query("UPDATE reminders SET isDeleted = 1, deletedAt = :timestamp WHERE id IN (:ids)")
    suspend fun softDeleteReminders(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    /**
     * Undelete a reminder (restore to original status)
     */
    @Query("UPDATE reminders SET isDeleted = 0, deletedAt = null WHERE id = :id")
    suspend fun undeleteReminder(id: Long)

    /**
     * Permanently delete old reminders (older than cutoff time)
     */
    @Query("DELETE FROM reminders WHERE isDeleted = 1 AND deletedAt < :cutoffTime")
    suspend fun purgeOldDeleted(cutoffTime: Long)

    /**
     * Permanently delete excess deleted reminders (keep only 20 most recent)
     */
    @Query("""
        DELETE FROM reminders 
        WHERE isDeleted = 1 
        AND id NOT IN (
            SELECT id FROM reminders 
            WHERE isDeleted = 1 
            ORDER BY deletedAt DESC 
            LIMIT 20
        )
    """)
    suspend fun purgeExcessDeleted()

    /**
     * Permanently delete all deleted reminders
     */
    @Query("DELETE FROM reminders WHERE isDeleted = 1")
    suspend fun permanentlyDeleteAll()

    /**
     * Soft delete all future reminders in a recurring group
     */
    @Query("""
        UPDATE reminders 
        SET isDeleted = 1, deletedAt = :timestamp 
        WHERE recurringGroupId = :groupId 
        AND dateTime >= :currentTime 
        AND id != :excludeId
        AND isDeleted = 0
    """)
    suspend fun softDeleteFutureRemindersInGroup(
        groupId: String,
        currentTime: Long,
        excludeId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    // ===== SYNC METHODS (for cloud sync) =====

    /**
     * Get all reminders for sync (returns list instead of Flow)
     */
    @Query("SELECT * FROM reminders")
    suspend fun getAllRemindersSync(): List<Reminder>

    /**
     * Get reminder by ID for sync (returns directly instead of Flow)
     */
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderByIdSync(id: Long): Reminder?
}