package com.example.myreminders_claude2.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // Get all categories
    @Query("SELECT * FROM categories ORDER BY isMainCategory DESC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    // Get main categories only
    @Query("SELECT * FROM categories WHERE isMainCategory = 1 ORDER BY name ASC")
    fun getMainCategories(): Flow<List<Category>>

    // Get subcategories for a specific main category
    @Query("SELECT * FROM categories WHERE parentCategoryId = :mainCategoryId ORDER BY name ASC")
    fun getSubcategoriesForCategory(mainCategoryId: Long): Flow<List<Category>>

    // Get category by ID
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    // Get category by name
    @Query("SELECT * FROM categories WHERE name = :name AND isMainCategory = :isMain")
    suspend fun getCategoryByName(name: String, isMain: Boolean): Category?

    // Insert category
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    // Insert multiple categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    // Update category
    @Update
    suspend fun updateCategory(category: Category)

    // Delete category (only if not preset)
    @Delete
    suspend fun deleteCategory(category: Category)

    // Delete category by ID (only if not preset)
    @Query("DELETE FROM categories WHERE id = :categoryId AND isPreset = 0")
    suspend fun deleteCategoryById(categoryId: Long)

    // Check if any categories exist
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    // Get all custom (non-preset) categories
    @Query("SELECT * FROM categories WHERE isPreset = 0 ORDER BY name ASC")
    fun getCustomCategories(): Flow<List<Category>>

    // ===== SYNC METHODS (for cloud sync) =====

    /**
     * Get all categories for sync (returns list instead of Flow)
     */
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<Category>

    /**
     * Get category by ID for sync (returns directly instead of Flow)
     */
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryByIdSync(id: Long): Category?
}