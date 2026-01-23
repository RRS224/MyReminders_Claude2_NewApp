package com.example.myreminders_claude2.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myreminders_claude2.alarm.AlarmScheduler
import com.example.myreminders_claude2.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ReminderDatabase.getDatabase(application)
    private val repository = ReminderRepository(database.reminderDao(), database.categoryDao())
    private val alarmScheduler = AlarmScheduler(application)

    // ===== REMINDER FLOWS =====

    val allActiveReminders: Flow<List<Reminder>> = repository.allActiveReminders
    val completedReminders: Flow<List<Reminder>> = repository.completedReminders

    // ===== CATEGORY FLOWS =====

    val allCategories: Flow<List<Category>> = repository.allCategories
    val mainCategories: Flow<List<Category>> = repository.mainCategories
    val customCategories: Flow<List<Category>> = repository.customCategories

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

            val id = repository.insertReminder(reminder)
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
            val existing = repository.getReminderById(id)
            existing?.let { reminder ->
                // Cancel old alarm
                alarmScheduler.cancelAlarm(reminder.id)

                // Check if this is part of a recurring group
                if (reminder.recurringGroupId != null) {
                    // Update all future reminders in the group
                    repository.updateFutureRemindersInGroup(
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
                    subCategory = subCategory
                )
                repository.updateReminder(updated)

                // Schedule new alarm
                alarmScheduler.scheduleAlarm(updated)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(reminder.id)
            repository.deleteReminder(reminder)
        }
    }

    fun deleteReminderWithRecurrenceCheck(
        reminder: Reminder,
        deleteAllFuture: Boolean
    ) {
        viewModelScope.launch {
            if (deleteAllFuture && reminder.recurringGroupId != null) {
                // Delete all future reminders in the group
                val futureReminders = repository.getFutureRemindersInGroup(
                    reminder.recurringGroupId,
                    System.currentTimeMillis()
                )

                futureReminders.forEach { futureReminder ->
                    alarmScheduler.cancelAlarm(futureReminder.id)
                }

                repository.deleteFutureRemindersInGroup(
                    reminder.recurringGroupId,
                    System.currentTimeMillis(),
                    reminder.id
                )
            }

            // Delete this reminder
            alarmScheduler.cancelAlarm(reminder.id)
            repository.deleteReminder(reminder)
        }
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return repository.getReminderById(id)
    }

    fun markAsCompleted(id: Long, isCompleted: Boolean, reason: String) {
        viewModelScope.launch {
            val reminder = repository.getReminderById(id)
            reminder?.let {
                // Cancel the alarm
                alarmScheduler.cancelAlarm(it.id)

                // Mark as completed
                repository.markAsCompleted(id, isCompleted, System.currentTimeMillis(), reason)

                // If it's a recurring reminder, create the next occurrence
                if (it.recurrenceType != RecurrenceType.ONE_TIME && it.recurringGroupId != null) {
                    createNextRecurrence(it)
                }
            }
        }
    }

    fun snoozeReminder(id: Long) {
        viewModelScope.launch {
            val reminder = repository.getReminderById(id)
            reminder?.let {
                val newSnoozeCount = it.snoozeCount + 1

                if (newSnoozeCount >= 3) {
                    // Auto-dismiss after 3 snoozes
                    markAsCompleted(id, true, "AUTO_SNOOZED")
                } else {
                    // Update snooze count
                    repository.updateSnoozeCount(id, newSnoozeCount)

                    // Reschedule alarm for 5 minutes later
                    val newDateTime = it.dateTime + (5 * 60 * 1000)
                    val updated = it.copy(dateTime = newDateTime, snoozeCount = newSnoozeCount)
                    repository.updateReminder(updated)
                    alarmScheduler.scheduleAlarm(updated)
                }
            }
        }
    }

    fun clearAllCompleted() {
        viewModelScope.launch {
            repository.clearAllCompleted()
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

        val newId = repository.insertReminder(nextReminder)
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
        return repository.getActiveRemindersByRecurrenceType(type)
    }

    fun getCompletedRemindersByRecurrenceType(type: String): Flow<List<Reminder>> {
        return repository.getCompletedRemindersByRecurrenceType(type)
    }

    // Get reminders by category
    fun getActiveRemindersByCategory(category: String): Flow<List<Reminder>> {
        return repository.getActiveRemindersByCategory(category)
    }

    fun getCompletedRemindersByCategory(category: String): Flow<List<Reminder>> {
        return repository.getCompletedRemindersByCategory(category)
    }

    // ===== CATEGORY METHODS =====

    fun getSubcategoriesForCategory(mainCategoryId: Long): Flow<List<Category>> {
        return repository.getSubcategoriesForCategory(mainCategoryId)
    }

    suspend fun getCategoryById(categoryId: Long): Category? {
        return repository.getCategoryById(categoryId)
    }

    suspend fun getCategoryByName(name: String, isMain: Boolean): Category? {
        return repository.getCategoryByName(name, isMain)
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
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
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
                val activeRemindersWithCategory = repository.getActiveRemindersByCategory(category.name).first()
                val completedRemindersWithCategory = repository.getCompletedRemindersByCategory(category.name).first()

                // Update active reminders
                activeRemindersWithCategory.forEach { reminder ->
                    val updated = reminder.copy(
                        mainCategory = CategoryDefaults.PERSONAL,
                        subCategory = null
                    )
                    repository.updateReminder(updated)
                }

                // Update completed reminders
                completedRemindersWithCategory.forEach { reminder ->
                    val updated = reminder.copy(
                        mainCategory = CategoryDefaults.PERSONAL,
                        subCategory = null
                    )
                    repository.updateReminder(updated)
                }
            } else {
                // Delete all reminders with this category
                val activeRemindersWithCategory = repository.getActiveRemindersByCategory(category.name).first()
                val completedRemindersWithCategory = repository.getCompletedRemindersByCategory(category.name).first()

                // Delete active reminders
                activeRemindersWithCategory.forEach { reminder ->
                    alarmScheduler.cancelAlarm(reminder.id)
                    repository.deleteReminder(reminder)
                }

                // Delete completed reminders
                completedRemindersWithCategory.forEach { reminder ->
                    repository.deleteReminder(reminder)
                }
            }

            // Delete the category
            repository.deleteCategory(category)
            onComplete()
        }
    }
}