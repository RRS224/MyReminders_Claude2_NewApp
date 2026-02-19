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
    entities = [Reminder::class, Category::class, Template::class, PermanentlyDeleted::class],
    version = 7, // ✅ Bumped from 6 → 7 for index migration
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceType TEXT NOT NULL DEFAULT 'ONE_TIME'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceDayOfWeek INTEGER")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurrenceDayOfMonth INTEGER")
                database.execSQL("ALTER TABLE reminders ADD COLUMN recurringGroupId TEXT")
                database.execSQL("ALTER TABLE reminders ADD COLUMN mainCategory TEXT NOT NULL DEFAULT 'PERSONAL'")
                database.execSQL("ALTER TABLE reminders ADD COLUMN subCategory TEXT")
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS templates (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        mainCategory TEXT NOT NULL,
                        subCategory TEXT,
                        recurrenceType TEXT NOT NULL,
                        recurrenceInterval INTEGER NOT NULL,
                        isVoiceEnabled INTEGER NOT NULL,
                        usageCount INTEGER NOT NULL,
                        lastUsedAt INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reminders ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE reminders ADD COLUMN deletedAt INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE reminders ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("UPDATE reminders SET updatedAt = createdAt")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS permanently_deleted (
                        reminderId INTEGER PRIMARY KEY NOT NULL
                    )
                """.trimIndent())
            }
        }

        // ✅ NEW: Migration 6 → 7 — adds indices for query performance.
        // These speed up the most common DAO queries (active reminders list,
        // completed tab, deleted tab, recurring group lookups, category filters).
        // Existing users' data is untouched — indices are additive only.
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Speeds up: getAllActiveReminders(), getAllActiveRemindersSync()
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_active ON reminders (isDeleted, isCompleted, dateTime)"
                )
                // Speeds up: getCompletedReminders(), getCompletedRemindersByCategory/RecurrenceType()
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_completed ON reminders (isCompleted, isDeleted, completedAt)"
                )
                // Speeds up: getDeletedReminders(), purgeOldDeleted(), purgeExcessDeleted()
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_deleted ON reminders (isDeleted, deletedAt)"
                )
                // Speeds up: getFutureRemindersInGroup(), getRemindersInRecurringGroup(),
                //            deleteFutureRemindersInGroup(), getActiveCountInRecurringGroup()
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_group ON reminders (recurringGroupId, dateTime)"
                )
                // Speeds up: getActiveRemindersByCategory(), getRemindersCountByCategory(),
                //            updateCategoryForAllReminders()
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_category ON reminders (mainCategory)"
                )
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "reminder_database_v5"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7  // ✅ New index migration
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

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
                        val count = database.categoryDao().getCategoryCount()
                        if (count == 0) {
                            populateCategories(database.categoryDao())
                        }
                    }
                }
            }

            suspend fun populateCategories(categoryDao: CategoryDao) {
                categoryDao.insertCategories(PresetCategories.getAllPresetCategories())
            }
        }
    }
}
