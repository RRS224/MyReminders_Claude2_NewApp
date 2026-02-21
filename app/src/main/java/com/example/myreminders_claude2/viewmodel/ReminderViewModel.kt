package com.example.myreminders_claude2.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myreminders_claude2.alarm.AlarmScheduler
import com.example.myreminders_claude2.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReminderDatabase.getDatabase(application)

    // Initialize SyncManager
    private val syncManager = SyncManager(
        firestore = FirebaseFirestore.getInstance(),
        auth = FirebaseAuth.getInstance(),
        reminderDao = database.reminderDao(),
        categoryDao = database.categoryDao(),
        templateDao = database.templateDao()
    )

    private val reminderRepository = ReminderRepository(
        database.reminderDao(),
        database.categoryDao(),
        syncManager
    )
    private val templateRepository = TemplateRepository(database.templateDao())
    private val alarmScheduler = AlarmScheduler(application)

    init {
        // Auto-purge deleted reminders on app start
        autoPurgeDeleted()

        // Start sync if user is already logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            startSync()
        }
    }

    // ===== SYNC METHODS =====

    /**
     * Start cloud sync (call when user signs in)
     */
    fun startSync() {
        syncManager.startSync()
    }

    /**
     * Stop cloud sync (call when user signs out)
     */
    fun stopSync() {
        syncManager.stopSync()
    }

    /**
     * Retry uploading any local reminders not yet in Firestore.
     * Called every time the app comes to foreground to handle
     * uploads killed by Samsung battery optimisation.
     */
    fun syncNow() {
        syncManager.syncNow()
    }

    // ===== REMINDER FLOWS =====

    val allActiveReminders = reminderRepository.allActiveReminders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val completedReminders = reminderRepository.completedReminders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val deletedReminders = reminderRepository.deletedReminders
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ===== CATEGORY FLOWS =====

    val allCategories = reminderRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val mainCategories = reminderRepository.mainCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val customCategories = reminderRepository.customCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ===== TEMPLATE FLOWS =====

    val allTemplates: Flow<List<Template>> = templateRepository.allTemplates
    val mostUsedTemplates: Flow<List<Template>> = templateRepository.mostUsedTemplates
    val recentlyUsedTemplates: Flow<List<Template>> = templateRepository.recentlyUsedTemplates

    // ===== REMINDER METHODS =====

    fun addReminder(
        title: String,
        notes: String,
        dateTime: Long,
        recurrenceType: String = RecurrenceType.ONE_TIME,
        recurrenceInterval: Int = 1,
        recurrenceDayOfWeek: Int? = null,
        recurrenceDayOfMonth: Int? = null,
        mainCategory: String = CategoryDefaults.PERSONAL,
        subCategory: String? = null,
        isVoiceEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val recurringGroupId = if (recurrenceType != RecurrenceType.ONE_TIME) {
                UUID.randomUUID().toString()
            } else null

            val reminder = Reminder(
                title = title,
                notes = notes,
                dateTime = dateTime,
                isVoiceEnabled = isVoiceEnabled,
                recurrenceType = recurrenceType,
                recurrenceInterval = recurrenceInterval,
                recurrenceDayOfWeek = recurrenceDayOfWeek,
                recurrenceDayOfMonth = recurrenceDayOfMonth,
                recurringGroupId = recurringGroupId,
                mainCategory = mainCategory,
                subCategory = subCategory
            )

            val id = reminderRepository.insertReminder(reminder)
            val savedReminder = reminder.copy(id = id)
            alarmScheduler.scheduleAlarm(savedReminder)
        }
    }

    fun updateReminder(
        id: Long,
        title: String,
        notes: String,
        dateTime: Long,
        mainCategory: String = CategoryDefaults.PERSONAL,
        subCategory: String? = null
    ) {
        viewModelScope.launch {
            val existing = reminderRepository.getReminderById(id)
            existing?.let { reminder ->
                // Cancel old alarm
                alarmScheduler.cancelAlarm(reminder.id)

                // Check if this is part of a recurring group
                if (reminder.recurringGroupId != null) {
                    // Update all future reminders in the group
                    reminderRepository.updateFutureRemindersInGroup(
                        groupId = reminder.recurringGroupId,
                        currentTime = System.currentTimeMillis(),
                        title = title,
                        notes = notes,
                        mainCategory = mainCategory,
                        subCategory = subCategory
                    )
                }

                // Update this specific reminder
                val updated = reminder.copy(
                    title = title,
                    notes = notes,
                    dateTime = dateTime,
                    mainCategory = mainCategory,
                    subCategory = subCategory,
                    isDeleted = false,  // Always undelete when updating
                    deletedAt = null,
                    isCompleted = false,
                    completedAt = null
                )
                reminderRepository.updateReminder(updated)

                // Schedule new alarm
                alarmScheduler.scheduleAlarm(updated)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(reminder.id)
            reminderRepository.softDeleteReminder(reminder.id)

            // Auto-purge old/excess deleted reminders
            reminderRepository.autoPurgeDeleted()
        }
    }

    fun deleteReminderWithRecurrenceCheck(
        reminder: Reminder,
        deleteAllFuture: Boolean
    ) {
        viewModelScope.launch {
            if (deleteAllFuture && reminder.recurringGroupId != null) {
                // Soft delete all future reminders in the group
                val futureReminders = reminderRepository.getFutureRemindersInGroup(
                    reminder.recurringGroupId,
                    System.currentTimeMillis()
                )

                futureReminders.forEach { futureReminder ->
                    alarmScheduler.cancelAlarm(futureReminder.id)
                }
            }

            // Soft delete this reminder (and all future if requested)
            alarmScheduler.cancelAlarm(reminder.id)
            reminderRepository.softDeleteReminderWithRecurrenceCheck(reminder, deleteAllFuture)

            // Auto-purge old/excess deleted reminders
            reminderRepository.autoPurgeDeleted()
        }
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return reminderRepository.getReminderById(id)
    }

    fun markReminderCompleted(id: Long, reason: String = "DISMISSED") {
        viewModelScope.launch {
            reminderRepository.markAsCompleted(id, true, System.currentTimeMillis(), reason)
        }
    }

    fun markAsCompleted(id: Long, isCompleted: Boolean, reason: String) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(id)
            reminder?.let {
                // Cancel the alarm
                alarmScheduler.cancelAlarm(it.id)

                // Mark as completed
                reminderRepository.markAsCompleted(id, isCompleted, System.currentTimeMillis(), reason)

                // If it's a recurring reminder, create the next occurrence
                if (it.recurrenceType != RecurrenceType.ONE_TIME && it.recurringGroupId != null) {
                    createNextRecurrence(it)
                }
            }
        }
    }

    fun snoozeReminder(id: Long) {
        viewModelScope.launch {
            val reminder = reminderRepository.getReminderById(id)
            reminder?.let {
                val newSnoozeCount = it.snoozeCount + 1

                if (newSnoozeCount >= 3) {
                    // Auto-dismiss after 3 snoozes
                    markAsCompleted(id, true, "AUTO_SNOOZED")
                } else {
                    // Update snooze count
                    reminderRepository.updateSnoozeCount(id, newSnoozeCount)

                    // Reschedule alarm for 5 minutes later
                    val newDateTime = it.dateTime + (5 * 60 * 1000)
                    val updated = it.copy(dateTime = newDateTime, snoozeCount = newSnoozeCount)
                    reminderRepository.updateReminder(updated)
                    alarmScheduler.scheduleAlarm(updated)
                }
            }
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            reminderRepository.clearAllCompleted()
        }
    }

    // ===== RECURRENCE METHODS =====

    private suspend fun createNextRecurrence(completedReminder: Reminder) {
        val nextDateTime = calculateNextOccurrence(
            currentDateTime = completedReminder.dateTime,
            recurrenceType = completedReminder.recurrenceType,
            recurrenceInterval = completedReminder.recurrenceInterval,
            recurrenceDayOfWeek = completedReminder.recurrenceDayOfWeek,
            recurrenceDayOfMonth = completedReminder.recurrenceDayOfMonth
        )

        val nextReminder = completedReminder.copy(
            id = 0, // New ID will be auto-generated
            dateTime = nextDateTime,
            isCompleted = false,
            completedAt = null,
            dismissalReason = null,
            snoozeCount = 0
        )

        val newId = reminderRepository.insertReminder(nextReminder)
        val savedNextReminder = nextReminder.copy(id = newId)
        alarmScheduler.scheduleAlarm(savedNextReminder)
    }

    private fun calculateNextOccurrence(
        currentDateTime: Long,
        recurrenceType: String,
        recurrenceInterval: Int,
        recurrenceDayOfWeek: Int?,
        recurrenceDayOfMonth: Int?
    ): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDateTime

        when (recurrenceType) {
            RecurrenceType.HOURLY -> {
                calendar.add(Calendar.HOUR_OF_DAY, recurrenceInterval)
            }

            RecurrenceType.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, recurrenceInterval)
            }

            RecurrenceType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, recurrenceInterval)
            }

            RecurrenceType.MONTHLY -> {
                calendar.add(Calendar.MONTH, recurrenceInterval)

                // Handle day of month (e.g., 31st in February)
                if (recurrenceDayOfMonth != null) {
                    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val targetDay = minOf(recurrenceDayOfMonth, maxDay)
                    calendar.set(Calendar.DAY_OF_MONTH, targetDay)
                }
            }

            RecurrenceType.ANNUAL -> {
                calendar.add(Calendar.YEAR, recurrenceInterval)
            }
        }

        return calendar.timeInMillis
    }

    // Get reminders by recurrence type
    fun getActiveRemindersByRecurrenceType(type: String): Flow<List<Reminder>> {
        return reminderRepository.getActiveRemindersByRecurrenceType(type)
    }

    fun getCompletedRemindersByRecurrenceType(type: String): Flow<List<Reminder>> {
        return reminderRepository.getCompletedRemindersByRecurrenceType(type)
    }

    // Get reminders by category
    fun getActiveRemindersByCategory(category: String): Flow<List<Reminder>> {
        return reminderRepository.getActiveRemindersByCategory(category)
    }

    fun getCompletedRemindersByCategory(category: String): Flow<List<Reminder>> {
        return reminderRepository.getCompletedRemindersByCategory(category)
    }

    // ===== CATEGORY METHODS =====

    fun getSubcategoriesForCategory(mainCategoryId: Long): Flow<List<Category>> {
        return reminderRepository.getSubcategoriesForCategory(mainCategoryId)
    }

    suspend fun getCategoryById(categoryId: Long): Category? {
        return reminderRepository.getCategoryById(categoryId)
    }

    suspend fun getCategoryByName(name: String, isMain: Boolean): Category? {
        return reminderRepository.getCategoryByName(name, isMain)
    }

    fun addCategory(
        name: String,
        isMainCategory: Boolean,
        parentCategoryId: Long? = null,
        colorHex: String = "#6200EE",
        iconName: String = "custom"
    ) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                isMainCategory = isMainCategory,
                parentCategoryId = parentCategoryId,
                isPreset = false,
                colorHex = colorHex,
                iconName = iconName
            )
            reminderRepository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            reminderRepository.updateCategory(category)
        }
    }

    fun deleteCategoryWithReminders(
        category: Category,
        moveRemindersToUncategorized: Boolean,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            if (category.isPreset) {
                // Cannot delete preset categories
                return@launch
            }

            if (moveRemindersToUncategorized) {
                // Move all reminders with this category to "PERSONAL" (uncategorized)
                val activeRemindersWithCategory = reminderRepository.getActiveRemindersByCategory(category.name).first()
                val completedRemindersWithCategory = reminderRepository.getCompletedRemindersByCategory(category.name).first()

                // Update active reminders
                activeRemindersWithCategory.forEach { reminder ->
                    val updated = reminder.copy(
                        mainCategory = CategoryDefaults.PERSONAL,
                        subCategory = null
                    )
                    reminderRepository.updateReminder(updated)
                }

                // Update completed reminders
                completedRemindersWithCategory.forEach { reminder ->
                    val updated = reminder.copy(
                        mainCategory = CategoryDefaults.PERSONAL,
                        subCategory = null
                    )
                    reminderRepository.updateReminder(updated)
                }
            } else {
                // Delete all reminders with this category (SOFT DELETE)
                val activeRemindersWithCategory = reminderRepository.getActiveRemindersByCategory(category.name).first()
                val completedRemindersWithCategory = reminderRepository.getCompletedRemindersByCategory(category.name).first()

                // Soft delete active reminders
                activeRemindersWithCategory.forEach { reminder ->
                    alarmScheduler.cancelAlarm(reminder.id)
                    reminderRepository.softDeleteReminder(reminder.id)
                }

                // Soft delete completed reminders
                completedRemindersWithCategory.forEach { reminder ->
                    reminderRepository.softDeleteReminder(reminder.id)
                }
            }

            // Delete the category
            reminderRepository.deleteCategory(category)
            onComplete()
        }
    }

    // ===== CATEGORY MANAGEMENT METHODS =====

    /**
     * Get count of reminders using a specific category
     */
    suspend fun getRemindersCountByCategory(category: String): Int {
        return reminderRepository.getRemindersCountByCategory(category)
    }

    /**
     * Update category for all reminders using oldCategory
     */
    suspend fun updateCategoryForAllReminders(oldCategory: String, newCategory: String) {
        reminderRepository.updateCategoryForAllReminders(oldCategory, newCategory)
        // Also update templates
        templateRepository.updateCategoryForAllTemplates(oldCategory, newCategory)
    }

    // ===== TEMPLATE METHODS =====

    /**
     * Get template by ID
     */
    suspend fun getTemplateById(id: Long): Template? {
        return templateRepository.getTemplateById(id)
    }

    /**
     * Get templates by category
     */
    fun getTemplatesByCategory(category: String): Flow<List<Template>> {
        return templateRepository.getTemplatesByCategory(category)
    }

    /**
     * Search templates
     */
    fun searchTemplates(query: String): Flow<List<Template>> {
        return templateRepository.searchTemplates(query)
    }

    /**
     * Get template count
     */
    suspend fun getTemplateCount(): Int {
        return templateRepository.getTemplateCount()
    }

    /**
     * Save reminder as template
     */
    fun saveAsTemplate(
        name: String,
        title: String,
        notes: String,
        mainCategory: String,
        subCategory: String?,
        recurrenceType: String,
        recurrenceInterval: Int,
        isVoiceEnabled: Boolean
    ) {
        viewModelScope.launch {
            val template = Template(
                name = name,
                title = title,
                notes = notes,
                mainCategory = mainCategory,
                subCategory = subCategory,
                recurrenceType = recurrenceType,
                recurrenceInterval = recurrenceInterval,
                isVoiceEnabled = isVoiceEnabled
            )
            templateRepository.insertTemplate(template)
        }
    }

    /**
     * Create reminder from template
     */
    fun createReminderFromTemplate(template: Template, dateTime: Long) {
        viewModelScope.launch {
            // Increment template usage count
            templateRepository.incrementUsageCount(template.id)

            // Create reminder
            addReminder(
                title = template.title,
                notes = template.notes,
                dateTime = dateTime,
                recurrenceType = template.recurrenceType,
                recurrenceInterval = template.recurrenceInterval,
                mainCategory = template.mainCategory,
                subCategory = template.subCategory,
                isVoiceEnabled = template.isVoiceEnabled
            )
        }
    }

    /**
     * Update template
     */
    fun updateTemplate(template: Template) {
        viewModelScope.launch {
            templateRepository.updateTemplate(template)
        }
    }

    /**
     * Delete template
     */
    fun deleteTemplate(template: Template) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template)
        }
    }

    /**
     * Delete all templates
     */
    fun deleteAllTemplates() {
        viewModelScope.launch {
            templateRepository.deleteAllTemplates()
        }
    }

    // ===== DELETED REMINDERS METHODS =====

    /**
     * Get count of deleted reminders
     */
    suspend fun getDeletedCount(): Int {
        return reminderRepository.getDeletedCount()
    }

    /**
     * Undelete a reminder (restore to original status)
     */
    fun undeleteReminderById(id: Long) {
        viewModelScope.launch {
            reminderRepository.undeleteReminder(id)
        }
    }
    fun undeleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            reminderRepository.undeleteReminder(reminder.id)

            // If it was active, check if we need to update the time
            if (!reminder.isCompleted) {
                val now = System.currentTimeMillis()
                if (reminder.dateTime <= now) {
                    // Time is in the past - update to 1 hour from now
                    val updatedReminder = reminder.copy(
                        dateTime = now + (60 * 60 * 1000), // 1 hour from now
                        isDeleted = false,
                        deletedAt = null,
                        updatedAt = now
                    )
                    reminderRepository.updateReminder(updatedReminder)
                    alarmScheduler.scheduleAlarm(updatedReminder)
                } else {
                    // Time is still in the future - just reschedule
                    alarmScheduler.scheduleAlarm(reminder)
                }
            }
        }
    }
    /**
     * Permanently delete a single reminder
     */
    fun softDeleteReminder(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository.softDeleteReminder(reminderId)
        }
    }

    fun permanentlyDeleteSingle(reminderId: Long) {
        viewModelScope.launch {
            reminderRepository.permanentlyDeleteReminder(reminderId)
        }
    }

    /**
     * Permanently delete all deleted reminders
     */
    fun permanentlyDeleteAll() {
        viewModelScope.launch {
            reminderRepository.permanentlyDeleteAll()
        }
    }

    /**
     * Auto-purge deleted reminders (called on app start and after deletes)
     */
    fun autoPurgeDeleted() {
        viewModelScope.launch {
            reminderRepository.autoPurgeDeleted()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Stop sync when ViewModel is destroyed
        stopSync()
    }
}