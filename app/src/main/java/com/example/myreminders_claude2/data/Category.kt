package com.example.myreminders_claude2.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val isMainCategory: Boolean,
    val parentCategoryId: Long? = null, // null if main category, references parent if subcategory
    val isPreset: Boolean, // true = can't delete (Work, Personal, Health, Finance)
    val colorHex: String = "#6200EE", // Category color
    val iconName: String = "work" // Icon identifier
)

// Preset category data
object PresetCategories {
    // Main categories with colors
    val WORK = Category(
        id = 1,
        name = "Work",
        isMainCategory = true,
        parentCategoryId = null,
        isPreset = true,
        colorHex = "#1976D2", // Blue
        iconName = "work"
    )

    val PERSONAL = Category(
        id = 2,
        name = "Personal",
        isMainCategory = true,
        parentCategoryId = null,
        isPreset = true,
        colorHex = "#4CAF50", // Green
        iconName = "home"
    )

    val HEALTH = Category(
        id = 3,
        name = "Health",
        isMainCategory = true,
        parentCategoryId = null,
        isPreset = true,
        colorHex = "#E91E63", // Pink
        iconName = "health"
    )

    val FINANCE = Category(
        id = 4,
        name = "Finance",
        isMainCategory = true,
        parentCategoryId = null,
        isPreset = true,
        colorHex = "#FF9800", // Orange
        iconName = "finance"
    )

    // Work subcategories (parent = 1)
    val WORK_CALL = Category(id = 11, name = "Call", isMainCategory = false, parentCategoryId = 1, isPreset = true, colorHex = "#1976D2", iconName = "call")
    val WORK_MEETING = Category(id = 12, name = "Meeting", isMainCategory = false, parentCategoryId = 1, isPreset = true, colorHex = "#1976D2", iconName = "meeting")
    val WORK_EMAIL = Category(id = 13, name = "Email", isMainCategory = false, parentCategoryId = 1, isPreset = true, colorHex = "#1976D2", iconName = "email")
    val WORK_DEADLINE = Category(id = 14, name = "Deadline", isMainCategory = false, parentCategoryId = 1, isPreset = true, colorHex = "#1976D2", iconName = "deadline")
    val WORK_REPORT = Category(id = 15, name = "Report", isMainCategory = false, parentCategoryId = 1, isPreset = true, colorHex = "#1976D2", iconName = "report")
    val WORK_TASK = Category(id = 16, name = "Task", isMainCategory = false, parentCategoryId = 1, isPreset = true, colorHex = "#1976D2", iconName = "task")

    // Personal subcategories (parent = 2)
    val PERSONAL_CALL = Category(id = 21, name = "Call", isMainCategory = false, parentCategoryId = 2, isPreset = true, colorHex = "#4CAF50", iconName = "call")
    val PERSONAL_ERRAND = Category(id = 22, name = "Errand", isMainCategory = false, parentCategoryId = 2, isPreset = true, colorHex = "#4CAF50", iconName = "errand")
    val PERSONAL_APPOINTMENT = Category(id = 23, name = "Appointment", isMainCategory = false, parentCategoryId = 2, isPreset = true, colorHex = "#4CAF50", iconName = "appointment")
    val PERSONAL_EVENT = Category(id = 24, name = "Event", isMainCategory = false, parentCategoryId = 2, isPreset = true, colorHex = "#4CAF50", iconName = "event")
    val PERSONAL_SOCIAL = Category(id = 25, name = "Social", isMainCategory = false, parentCategoryId = 2, isPreset = true, colorHex = "#4CAF50", iconName = "social")

    // Health subcategories (parent = 3)
    val HEALTH_MEDICATION = Category(id = 31, name = "Medication", isMainCategory = false, parentCategoryId = 3, isPreset = true, colorHex = "#E91E63", iconName = "medication")
    val HEALTH_EXERCISE = Category(id = 32, name = "Exercise", isMainCategory = false, parentCategoryId = 3, isPreset = true, colorHex = "#E91E63", iconName = "exercise")
    val HEALTH_DOCTOR = Category(id = 33, name = "Doctor", isMainCategory = false, parentCategoryId = 3, isPreset = true, colorHex = "#E91E63", iconName = "doctor")
    val HEALTH_CHECKUP = Category(id = 34, name = "Checkup", isMainCategory = false, parentCategoryId = 3, isPreset = true, colorHex = "#E91E63", iconName = "checkup")
    val HEALTH_THERAPY = Category(id = 35, name = "Therapy", isMainCategory = false, parentCategoryId = 3, isPreset = true, colorHex = "#E91E63", iconName = "therapy")

    // Finance subcategories (parent = 4)
    val FINANCE_BILL = Category(id = 41, name = "Bill", isMainCategory = false, parentCategoryId = 4, isPreset = true, colorHex = "#FF9800", iconName = "bill")
    val FINANCE_PAYMENT = Category(id = 42, name = "Payment", isMainCategory = false, parentCategoryId = 4, isPreset = true, colorHex = "#FF9800", iconName = "payment")
    val FINANCE_TAX = Category(id = 43, name = "Tax", isMainCategory = false, parentCategoryId = 4, isPreset = true, colorHex = "#FF9800", iconName = "tax")
    val FINANCE_BUDGET = Category(id = 44, name = "Budget", isMainCategory = false, parentCategoryId = 4, isPreset = true, colorHex = "#FF9800", iconName = "budget")
    val FINANCE_INVESTMENT = Category(id = 45, name = "Investment", isMainCategory = false, parentCategoryId = 4, isPreset = true, colorHex = "#FF9800", iconName = "investment")

    // Get all preset categories as a list
    fun getAllPresetCategories(): List<Category> {
        return listOf(
            WORK, PERSONAL, HEALTH, FINANCE,
            WORK_CALL, WORK_MEETING, WORK_EMAIL, WORK_DEADLINE, WORK_REPORT, WORK_TASK,
            PERSONAL_CALL, PERSONAL_ERRAND, PERSONAL_APPOINTMENT, PERSONAL_EVENT, PERSONAL_SOCIAL,
            HEALTH_MEDICATION, HEALTH_EXERCISE, HEALTH_DOCTOR, HEALTH_CHECKUP, HEALTH_THERAPY,
            FINANCE_BILL, FINANCE_PAYMENT, FINANCE_TAX, FINANCE_BUDGET, FINANCE_INVESTMENT
        )
    }
}