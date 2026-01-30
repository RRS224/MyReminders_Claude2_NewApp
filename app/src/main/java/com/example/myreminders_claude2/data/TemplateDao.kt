package com.example.myreminders_claude2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    // ===== QUERY OPERATIONS =====

    // Get all templates ordered by usage (most used first)
    @Query("SELECT * FROM templates ORDER BY usageCount DESC, name ASC")
    fun getAllTemplates(): Flow<List<Template>>

    // Get all templates ordered by name
    @Query("SELECT * FROM templates ORDER BY name ASC")
    fun getAllTemplatesByName(): Flow<List<Template>>

    // Get template by ID
    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getTemplateById(templateId: Long): Template?

    // Get templates by category
    @Query("SELECT * FROM templates WHERE mainCategory = :category ORDER BY usageCount DESC")
    fun getTemplatesByCategory(category: String): Flow<List<Template>>

    // Search templates by name or title
    @Query("SELECT * FROM templates WHERE name LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY usageCount DESC")
    fun searchTemplates(query: String): Flow<List<Template>>

    // Get most used templates (top 5)
    @Query("SELECT * FROM templates ORDER BY usageCount DESC LIMIT 5")
    fun getMostUsedTemplates(): Flow<List<Template>>

    // Get recently used templates (top 5)
    @Query("SELECT * FROM templates WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT 5")
    fun getRecentlyUsedTemplates(): Flow<List<Template>>

    // Count total templates
    @Query("SELECT COUNT(*) FROM templates")
    suspend fun getTemplateCount(): Int

    // ===== INSERT OPERATIONS =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: Template): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<Template>)

    // ===== UPDATE OPERATIONS =====

    @Update
    suspend fun updateTemplate(template: Template)

    // Increment usage count when template is used
    @Query("UPDATE templates SET usageCount = usageCount + 1, lastUsedAt = :timestamp WHERE id = :templateId")
    suspend fun incrementUsageCount(templateId: Long, timestamp: Long = System.currentTimeMillis())

    // Update category for templates when category is renamed
    @Query("UPDATE templates SET mainCategory = :newCategory WHERE mainCategory = :oldCategory")
    suspend fun updateCategoryForAllTemplates(oldCategory: String, newCategory: String)

    // ===== DELETE OPERATIONS =====

    @Delete
    suspend fun deleteTemplate(template: Template)

    @Query("DELETE FROM templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: Long)

    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()

    // Delete templates by category
    @Query("DELETE FROM templates WHERE mainCategory = :category")
    suspend fun deleteTemplatesByCategory(category: String)

    // ===== SYNC METHODS (for cloud sync) =====

    /**
     * Get all templates for sync (returns list instead of Flow)
     */
    @Query("SELECT * FROM templates")
    suspend fun getAllTemplatesSync(): List<Template>

    /**
     * Get template by ID for sync (returns directly instead of Flow)
     */
    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateByIdSync(id: Long): Template?
}