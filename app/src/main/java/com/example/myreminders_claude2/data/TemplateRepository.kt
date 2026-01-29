package com.example.myreminders_claude2.data

import kotlinx.coroutines.flow.Flow

class TemplateRepository(private val templateDao: TemplateDao) {

    // ===== QUERY METHODS =====

    // Get all templates (ordered by usage)
    val allTemplates: Flow<List<Template>> = templateDao.getAllTemplates()

    // Get all templates (ordered by name)
    val allTemplatesByName: Flow<List<Template>> = templateDao.getAllTemplatesByName()

    // Get template by ID
    suspend fun getTemplateById(id: Long): Template? {
        return templateDao.getTemplateById(id)
    }

    // Get templates by category
    fun getTemplatesByCategory(category: String): Flow<List<Template>> {
        return templateDao.getTemplatesByCategory(category)
    }

    // Search templates
    fun searchTemplates(query: String): Flow<List<Template>> {
        return templateDao.searchTemplates(query)
    }

    // Get most used templates
    val mostUsedTemplates: Flow<List<Template>> = templateDao.getMostUsedTemplates()

    // Get recently used templates
    val recentlyUsedTemplates: Flow<List<Template>> = templateDao.getRecentlyUsedTemplates()

    // Get template count
    suspend fun getTemplateCount(): Int {
        return templateDao.getTemplateCount()
    }

    // ===== INSERT METHODS =====

    suspend fun insertTemplate(template: Template): Long {
        return templateDao.insertTemplate(template)
    }

    suspend fun insertTemplates(templates: List<Template>) {
        templateDao.insertTemplates(templates)
    }

    // ===== UPDATE METHODS =====

    suspend fun updateTemplate(template: Template) {
        templateDao.updateTemplate(template)
    }

    suspend fun incrementUsageCount(templateId: Long) {
        templateDao.incrementUsageCount(templateId, System.currentTimeMillis())
    }

    suspend fun updateCategoryForAllTemplates(oldCategory: String, newCategory: String) {
        templateDao.updateCategoryForAllTemplates(oldCategory, newCategory)
    }

    // ===== DELETE METHODS =====

    suspend fun deleteTemplate(template: Template) {
        templateDao.deleteTemplate(template)
    }

    suspend fun deleteTemplateById(id: Long) {
        templateDao.deleteTemplateById(id)
    }

    suspend fun deleteAllTemplates() {
        templateDao.deleteAllTemplates()
    }

    suspend fun deleteTemplatesByCategory(category: String) {
        templateDao.deleteTemplatesByCategory(category)
    }
}