package com.example.myreminders_claude2.data

import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao
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
        return reminderDao.insertReminder(reminder)
    }

    // Update reminder
    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
    }

    // Delete reminder
    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.deleteReminder(reminder)
    }

    // Delete reminder by ID
    suspend fun deleteReminderById(id: Long) {
        reminderDao.deleteReminderById(id)
    }

    // Mark as completed
    suspend fun markAsCompleted(id: Long, isCompleted: Boolean, completedAt: Long, reason: String) {
        reminderDao.markAsCompleted(id, isCompleted, completedAt, reason)
    }

    // Update snooze count
    suspend fun updateSnoozeCount(id: Long, count: Int) {
        reminderDao.updateSnoozeCount(id, count)
    }

    // Clear all completed
    suspend fun clearAllCompleted() {
        reminderDao.clearAllCompleted()
    }

    // ===== NEW: RECURRENCE METHODS =====

    // Get all reminders in a recurring group
    fun getRemindersInRecurringGroup(groupId: String): Flow<List<Reminder>> {
        return reminderDao.getRemindersInRecurringGroup(groupId)
    }

    // Get future reminders in a recurring group
    suspend fun getFutureRemindersInGroup(groupId: String, currentTime: Long): List<Reminder> {
        return reminderDao.getFutureRemindersInGroup(groupId, currentTime)
    }

    // Delete all future reminders in a recurring group
    suspend fun deleteFutureRemindersInGroup(groupId: String, currentTime: Long, excludeId: Long) {
        reminderDao.deleteFutureRemindersInGroup(groupId, currentTime, excludeId)
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
        reminderDao.updateFutureRemindersInGroup(groupId, currentTime, title, notes, mainCategory, subCategory)
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

    // ===== NEW: CATEGORY METHODS =====

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
        return categoryDao.insertCategory(category)
    }

    // Insert multiple categories
    suspend fun insertCategories(categories: List<Category>) {
        categoryDao.insertCategories(categories)
    }

    // Update category
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    // Delete category
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }

    // Delete category by ID
    suspend fun deleteCategoryById(categoryId: Long) {
        categoryDao.deleteCategoryById(categoryId)
    }

    // Get custom categories
    val customCategories: Flow<List<Category>> = categoryDao.getCustomCategories()
}