package com.example.myreminders_claude2.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager? = null // Nullable to support offline mode
) {

    // ===== REMINDER METHODS =====

    // Get all active reminders
    val allActiveReminders: Flow<List<Reminder>> = reminderDao.getAllActiveReminders()

    // Get all completed reminders
    val completedReminders: Flow<List<Reminder>> = reminderDao.getCompletedReminders()

    // Get reminder by ID
    suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)
    }

    // Insert reminder
    suspend fun insertReminder(reminder: Reminder): Long {
        val now = System.currentTimeMillis()
        val reminderWithTimestamp = reminder.copy(updatedAt = now)
        val id = reminderDao.insertReminder(reminderWithTimestamp)
        // Sync to cloud
        syncManager?.uploadReminder(reminderWithTimestamp.copy(id = id))
        return id
    }

    // Update reminder
    suspend fun updateReminder(reminder: Reminder) {
        val now = System.currentTimeMillis()
        val reminderWithTimestamp = reminder.copy(updatedAt = now)
        reminderDao.updateReminder(reminderWithTimestamp)
        // Sync to cloud
        syncManager?.uploadReminder(reminderWithTimestamp)
    }

    // Delete reminder (HARD DELETE - only used internally)
    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
        // Delete from cloud
        syncManager?.deleteReminderFromCloud(reminder.id)
    }

    // Delete reminder by ID (HARD DELETE - only used internally)
    suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteReminderById(id)
        // Delete from cloud
        syncManager?.deleteReminderFromCloud(id)
    }

    // Mark as completed
    suspend fun markAsCompleted(id: Long, isCompleted: Boolean, completedAt: Long, reason: String) {
        reminderDao.markAsCompleted(id, isCompleted, completedAt, reason)
        // Sync updated reminder to cloud
        getReminderById(id)?.let { reminder ->
            syncManager?.uploadReminder(reminder)
        }
    }

    // Update snooze count
    suspend fun updateSnoozeCount(id: Long, count: Int) {
        reminderDao.updateSnoozeCount(id, count)
        // Sync updated reminder to cloud
        getReminderById(id)?.let { reminder ->
            syncManager?.uploadReminder(reminder)
        }
    }

    // Clear all completed (SOFT DELETE)
    suspend fun clearAllCompleted() {
        reminderDao.clearAllCompleted(System.currentTimeMillis())
        // Note: Cloud sync will handle this via the listener
    }

    // ===== RECURRENCE METHODS =====

    // Get all reminders in a recurring group
    fun getRemindersInRecurringGroup(groupId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersInRecurringGroup(groupId)
    }

    // Get future reminders in a recurring group
    suspend fun getFutureRemindersInGroup(groupId: String, currentTime: Long): List<Reminder> {
        return reminderDao.getFutureRemindersInGroup(groupId, currentTime)
    }

    // Delete all future reminders in a recurring group (SOFT DELETE)
    suspend fun deleteFutureRemindersInGroup(groupId: String, currentTime: Long, excludeId: Long) {
        reminderDao.deleteFutureRemindersInGroup(
            groupId,
            currentTime,
            excludeId,
            System.currentTimeMillis()
        )
        // Note: Cloud sync will handle this via the listener
    }

    // Update all future reminders in a recurring group
    suspend fun updateFutureRemindersInGroup(
        groupId: String,
        currentTime: Long,
        title: String,
        notes: String,
        mainCategory: String,
        subCategory: String?
    ) {
        reminderDao.updateFutureRemindersInGroup(
            groupId,
            currentTime,
            title,
            notes,
            mainCategory,
            subCategory
        )
        // Note: Cloud sync will handle this via the listener
    }

    // Get active reminders by recurrence type
    fun getActiveRemindersByRecurrenceType(type: String): Flow<List<Reminder>> {
        return reminderDao.getActiveRemindersByRecurrenceType(type)
    }

    // Get active reminders by category
    fun getActiveRemindersByCategory(category: String): Flow<List<Reminder>> {
        return reminderDao.getActiveRemindersByCategory(category)
    }

    // Get completed reminders by recurrence type
    fun getCompletedRemindersByRecurrenceType(type: String): Flow<List<Reminder>> {
        return reminderDao.getCompletedRemindersByRecurrenceType(type)
    }

    // Get completed reminders by category
    fun getCompletedRemindersByCategory(category: String): Flow<List<Reminder>> {
        return reminderDao.getCompletedRemindersByCategory(category)
    }

    // Check if recurring group has active instances
    suspend fun getActiveCountInRecurringGroup(groupId: String): Int {
        return reminderDao.getActiveCountInRecurringGroup(groupId)
    }

    // ===== CATEGORY METHODS =====

    // Get all categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    // Get main categories only
    val mainCategories: Flow<List<Category>> = categoryDao.getMainCategories()

    // Get subcategories for a main category
    fun getSubcategoriesForCategory(mainCategoryId: Long): Flow<List<Category>> {
        return categoryDao.getSubcategoriesForCategory(mainCategoryId)
    }

    // Get category by ID
    suspend fun getCategoryById(categoryId: Long): Category? {
        return categoryDao.getCategoryById(categoryId)
    }

    // Get category by name
    suspend fun getCategoryByName(name: String, isMain: Boolean): Category? {
        return categoryDao.getCategoryByName(name, isMain)
    }

    // Insert category
    suspend fun insertCategory(category: Category): Long {
        val id = categoryDao.insertCategory(category)
        // Sync to cloud
        syncManager?.uploadCategory(category.copy(id = id))
        return id
    }

    // Insert multiple categories
    suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories)
        // Sync to cloud
        categories.forEach { category ->
            syncManager?.uploadCategory(category)
        }
    }

    // Update category
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
        // Sync to cloud
        syncManager?.uploadCategory(category)
    }

    // Delete category
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
        // Note: Categories don't have cloud delete (they're preset or custom)
    }

    // Delete category by ID
    suspend fun deleteCategoryById(categoryId: Long) {
        categoryDao.deleteCategoryById(categoryId)
        // Note: Categories don't have cloud delete (they're preset or custom)
    }

    // Get custom categories
    val customCategories: Flow<List<Category>> = categoryDao.getCustomCategories()

    // ===== CATEGORY MANAGEMENT METHODS =====

    suspend fun getRemindersCountByCategory(category: String): Int {
        return reminderDao.getRemindersCountByCategory(category)
    }

    suspend fun updateCategoryForAllReminders(oldCategory: String, newCategory: String) {
        reminderDao.updateCategoryForAllReminders(oldCategory, newCategory)
        // Note: Cloud sync will handle this via the listener
    }

    // ===== DELETED REMINDERS METHODS =====

    /**
     * Get all deleted reminders
     */
    val deletedReminders: Flow<List<Reminder>> = reminderDao.getDeletedReminders()

    /**
     * Get count of deleted reminders
     */
    suspend fun getDeletedCount(): Int {
        return reminderDao.getDeletedCount()
    }

    /**
     * Soft delete a reminder (mark as deleted instead of removing)
     */
    suspend fun softDeleteReminder(id: Long) {
        reminderDao.softDeleteReminder(id, System.currentTimeMillis())
        // Sync to cloud
        getReminderById(id)?.let { reminder ->
            syncManager?.uploadReminder(reminder)
        }
    }

    /**
     * Soft delete multiple reminders
     */
    suspend fun softDeleteReminders(ids: List<Long>) {
        reminderDao.softDeleteReminders(ids, System.currentTimeMillis())
        // Sync to cloud
        ids.forEach { id ->
            getReminderById(id)?.let { reminder ->
                syncManager?.uploadReminder(reminder)
            }
        }
    }

    /**
     * Soft delete with recurrence check
     */
    suspend fun softDeleteReminderWithRecurrenceCheck(
        reminder: Reminder,
        deleteAllFuture: Boolean
    ) {
        if (deleteAllFuture && reminder.recurringGroupId != null) {
            // Soft delete all future reminders in the group
            reminderDao.softDeleteFutureRemindersInGroup(
                reminder.recurringGroupId,
                System.currentTimeMillis(),
                reminder.id,
                System.currentTimeMillis()
            )
        }

        // Soft delete this reminder
        reminderDao.softDeleteReminder(reminder.id, System.currentTimeMillis())

        // Sync to cloud
        getReminderById(reminder.id)?.let { updatedReminder ->
            syncManager?.uploadReminder(updatedReminder)
        }
    }

    /**
     * Undelete a reminder (restore to original status)
     */
    suspend fun undeleteReminder(id: Long) {
        reminderDao.undeleteReminder(id)
        // Sync to cloud
        getReminderById(id)?.let { reminder ->
            syncManager?.uploadReminder(reminder)
        }
    }

    /**
     * Purge old deleted reminders (older than 7 days)
     */
    suspend fun purgeOldDeleted() {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        reminderDao.purgeOldDeleted(sevenDaysAgo)
    }

    /**
     * Purge excess deleted reminders (keep only 20 most recent)
     */
    suspend fun purgeExcessDeleted() {
        reminderDao.purgeExcessDeleted()
    }

    /**
     * Auto-purge: Remove old and excess deleted reminders
     */
    suspend fun autoPurgeDeleted() {
        purgeOldDeleted()
        purgeExcessDeleted()
    }

    /**
     * Permanently delete a single reminder
     */
    suspend fun permanentlyDeleteReminder(reminderId: Long) {
        reminderDao.permanentlyDeleteReminder(reminderId)
    }

    /**
     * Permanently delete all deleted reminders
     */
    suspend fun permanentlyDeleteAll() {
        reminderDao.permanentlyDeleteAll()
    }
}
