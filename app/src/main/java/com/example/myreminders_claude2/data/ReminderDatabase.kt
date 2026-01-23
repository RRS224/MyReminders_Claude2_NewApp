package com.example.myreminders_claude2.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Reminder::class, Category::class],
    version = 2,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        // Migration from version 1 to version 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to reminders table
                // Note: isVoiceEnabled already exists, so we skip it
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'ONE_TIME'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceDayOfWeek INTEGER")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceDayOfMonth INTEGER")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurringGroupId TEXT")
                database.execSQL("ALTER TABLE reminders ADD COLUMN mainCategory TEXT NOT NULL DEFAULT 'PERSONAL'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN subCategory TEXT")

                // Create categories table
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                isMainCategory INTEGER NOT NULL,
                parentCategoryId INTEGER,
                isPreset INTEGER NOT NULL,
                colorHex TEXT NOT NULL,
                iconName TEXT NOT NULL
            )
        """.trimIndent())
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database_v3"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Callback to prepopulate categories on first run
        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateCategories(database.categoryDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Check if categories are already populated
                        val count = database.categoryDao().getCategoryCount()
                        if (count == 0) {
                            populateCategories(database.categoryDao())
                        }
                    }
                }
            }

            suspend fun populateCategories(categoryDao: CategoryDao) {
                // Insert all preset categories
                categoryDao.insertCategories(PresetCategories.getAllPresetCategories())
            }
        }
    }
}