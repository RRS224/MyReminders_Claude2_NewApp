package com.example.myreminders_claude2.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CustomCategory(
    val id: String,
    val name: String,
    val emoji: String
)

class CategoryManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("custom_categories", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CATEGORIES = "categories"
        const val MAX_CUSTOM_CATEGORIES = 10

        // Default categories (cannot be deleted)
        val DEFAULT_CATEGORIES = listOf("WORK", "PERSONAL", "HEALTH", "FINANCE")
    }

    private val _customCategories = MutableStateFlow<List<CustomCategory>>(emptyList())
    val customCategories: StateFlow<List<CustomCategory>> = _customCategories.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        val categoriesJson = prefs.getString(KEY_CATEGORIES, "[]") ?: "[]"
        val categories = parseCategories(categoriesJson)
        _customCategories.value = categories
    }

    private fun saveCategories() {
        val categoriesJson = _customCategories.value.joinToString(",", "[", "]") { category ->
            """{"id":"${category.id}","name":"${category.name}","emoji":"${category.emoji}"}"""
        }
        prefs.edit().putString(KEY_CATEGORIES, categoriesJson).apply()
    }

    private fun parseCategories(json: String): List<CustomCategory> {
        if (json == "[]") return emptyList()

        val categories = mutableListOf<CustomCategory>()
        val items = json.removeSurrounding("[", "]").split("},")

        for (item in items) {
            val cleanItem = item.replace("}", "").replace("{", "")
            val parts = cleanItem.split(",")

            var id = ""
            var name = ""
            var emoji = ""

            for (part in parts) {
                val keyValue = part.split(":")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().removeSurrounding("\"")
                    val value = keyValue[1].trim().removeSurrounding("\"")

                    when (key) {
                        "id" -> id = value
                        "name" -> name = value
                        "emoji" -> emoji = value
                    }
                }
            }

            if (id.isNotEmpty() && name.isNotEmpty() && emoji.isNotEmpty()) {
                categories.add(CustomCategory(id, name, emoji))
            }
        }

        return categories
    }

    /**
     * Add a new custom category
     * Returns true if successful, false if limit reached
     */
    fun addCategory(name: String, emoji: String): Boolean {
        if (_customCategories.value.size >= MAX_CUSTOM_CATEGORIES) {
            return false
        }

        val id = generateId()
        val newCategory = CustomCategory(id, name.uppercase(), emoji)
        _customCategories.value = _customCategories.value + newCategory
        saveCategories()
        return true
    }

    /**
     * Edit an existing category
     */
    fun editCategory(id: String, newName: String, newEmoji: String) {
        _customCategories.value = _customCategories.value.map { category ->
            if (category.id == id) {
                category.copy(name = newName.uppercase(), emoji = newEmoji)
            } else {
                category
            }
        }
        saveCategories()
    }

    /**
     * Delete a category
     */
    fun deleteCategory(id: String) {
        _customCategories.value = _customCategories.value.filter { it.id != id }
        saveCategories()
    }

    /**
     * Get all categories (default + custom)
     */
    fun getAllCategories(): List<Pair<String, String>> {
        val defaults = listOf(
            "WORK" to "💼",
            "PERSONAL" to "🏠",
            "HEALTH" to "❤️",
            "FINANCE" to "💰"
        )

        val customs = _customCategories.value.map { it.name to it.emoji }

        return defaults + customs
    }

    /**
     * Check if a category name already exists
     */
    fun categoryExists(name: String): Boolean {
        val upperName = name.uppercase()
        return DEFAULT_CATEGORIES.contains(upperName) ||
                _customCategories.value.any { it.name == upperName }
    }

    /**
     * Get category by name
     */
    fun getCategoryByName(name: String): CustomCategory? {
        return _customCategories.value.find { it.name == name.uppercase() }
    }

    /**
     * Check if category is custom (not default)
     */
    fun isCustomCategory(name: String): Boolean {
        return !DEFAULT_CATEGORIES.contains(name.uppercase())
    }

    private fun generateId(): String {
        return "cat_${System.currentTimeMillis()}"
    }
}