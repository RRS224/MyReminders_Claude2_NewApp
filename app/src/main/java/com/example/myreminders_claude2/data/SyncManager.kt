package com.example.myreminders_claude2.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class SyncManager(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val reminderDao: ReminderDao,
    private val categoryDao: CategoryDao,
    private val templateDao: TemplateDao
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var remindersListener: ListenerRegistration? = null
    private var categoriesListener: ListenerRegistration? = null
    private var templatesListener: ListenerRegistration? = null

    private val userId: String?
        get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "SyncManager"
        private const val COLLECTION_REMINDERS = "reminders"
        private const val COLLECTION_CATEGORIES = "categories"
        private const val COLLECTION_TEMPLATES = "templates"
    }

    fun startSync() {
        val uid = userId ?: run {
            Log.w(TAG, "No user logged in, skipping sync")
            return
        }

        Log.d(TAG, "Starting sync for user: $uid")

        scope.launch {
            syncLocalToCloud()
        }

        setupRealtimeListeners(uid)
    }
    fun stopSync() {
        Log.d(TAG, "Stopping sync")
        remindersListener?.remove()
        categoriesListener?.remove()
        templatesListener?.remove()
        scope.cancel()
    }

    private suspend fun syncLocalToCloud() {
        val uid = userId ?: return

        try {
            val localReminders = reminderDao.getAllRemindersSync()
            localReminders.forEach { reminder ->
                uploadReminder(reminder, uid)
            }

            val localCategories = categoryDao.getAllCategoriesSync()
            localCategories.forEach { category ->
                uploadCategory(category, uid)
            }

            val localTemplates = templateDao.getAllTemplatesSync()
            localTemplates.forEach { template ->
                uploadTemplate(template, uid)
            }

            Log.d(TAG, "Local to Cloud sync complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to cloud", e)
        }
    }
    suspend fun uploadReminder(reminder: Reminder, uid: String? = null) {
        val effectiveUid = uid ?: userId ?: return
        try {
            val firestoreReminder = reminder.toFirestore(effectiveUid)
            firestore.collection(COLLECTION_REMINDERS)
                .document(reminder.id.toString())
                .set(firestoreReminder)
                .await()
            Log.d(TAG, "Uploaded reminder: ${reminder.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload reminder", e)
        }
    }

    suspend fun uploadCategory(category: Category, uid: String? = null) {
        val effectiveUid = uid ?: userId ?: return
        try {
            val firestoreCategory = category.toFirestore(effectiveUid)
            firestore.collection(COLLECTION_CATEGORIES)
                .document(category.id.toString())
                .set(firestoreCategory)
                .await()
            Log.d(TAG, "Uploaded category: ${category.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload category", e)
        }
    }

    suspend fun uploadTemplate(template: Template, uid: String? = null) {
        val effectiveUid = uid ?: userId ?: return
        try {
            val firestoreTemplate = template.toFirestore(effectiveUid)
            firestore.collection(COLLECTION_TEMPLATES)
                .document(template.id.toString())
                .set(firestoreTemplate)
                .await()
            Log.d(TAG, "Uploaded template: ${template.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload template", e)
        }
    }

    private fun setupRealtimeListeners(uid: String) {
        remindersListener = firestore.collection(COLLECTION_REMINDERS)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Reminders listener error", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    scope.launch {
                        val firestoreReminder = change.document.toObject(FirestoreReminder::class.java)
                        handleReminderChange(firestoreReminder)
                    }
                }
            }

        categoriesListener = firestore.collection(COLLECTION_CATEGORIES)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Categories listener error", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    scope.launch {
                        val firestoreCategory = change.document.toObject(FirestoreCategory::class.java)
                        handleCategoryChange(firestoreCategory)
                    }
                }
            }

        templatesListener = firestore.collection(COLLECTION_TEMPLATES)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Templates listener error", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    scope.launch {
                        val firestoreTemplate = change.document.toObject(FirestoreTemplate::class.java)
                        handleTemplateChange(firestoreTemplate)
                    }
                }
            }

        Log.d(TAG, "Real-time listeners active")
    }

    private suspend fun handleReminderChange(firestoreReminder: FirestoreReminder) {
        val localReminder = firestoreReminder.toRoom()
        val existing = reminderDao.getReminderByIdSync(localReminder.id)

        if (existing == null) {
            // New reminder from cloud - add it
            reminderDao.insertReminder(localReminder)
            Log.d(TAG, "Downloaded new reminder: ${localReminder.title}")
        } else {
            // Only update if cloud version is genuinely newer
            if (localReminder.updatedAt > existing.updatedAt) {
                reminderDao.updateReminder(localReminder)
                Log.d(TAG, "Updated reminder from cloud (newer): ${localReminder.title}")
            } else {
                Log.d(TAG, "Skipped update - local is newer or same: ${localReminder.title}")
            }
        }
    }

    private suspend fun handleCategoryChange(firestoreCategory: FirestoreCategory) {
        val localCategory = firestoreCategory.toRoom()
        val existing = categoryDao.getCategoryByIdSync(localCategory.id)

        if (existing == null) {
            categoryDao.insertCategory(localCategory)
            Log.d(TAG, "Downloaded new category: ${localCategory.name}")
        }
    }

    private suspend fun handleTemplateChange(firestoreTemplate: FirestoreTemplate) {
        val localTemplate = firestoreTemplate.toRoom()
        val existing = templateDao.getTemplateByIdSync(localTemplate.id)

        if (existing == null) {
            templateDao.insertTemplate(localTemplate)
            Log.d(TAG, "Downloaded new template: ${localTemplate.title}")
        }
    }

    suspend fun deleteReminderFromCloud(reminderId: Long) {
        try {
            firestore.collection(COLLECTION_REMINDERS)
                .document(reminderId.toString())
                .delete()
                .await()
            Log.d(TAG, "Deleted reminder from cloud: $reminderId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete reminder from cloud", e)
        }
    }
}

// Extension functions for converting between Room and Firestore models

// Extension functions for converting between Room and Firestore models

fun Reminder.toFirestore(userId: String) = FirestoreReminder(
    id = id.toString(),
    title = title,
    description = notes,
    dateTime = dateTime.let { Timestamp(Date(it)) },
    category = mainCategory,
    isCompleted = isCompleted,
    isDeleted = isDeleted,
    createdAt = Timestamp(Date(createdAt)),
    updatedAt = Timestamp(Date(updatedAt)),
    userId = userId
)

fun FirestoreReminder.toRoom() = Reminder(
    id = id.toLongOrNull() ?: 0L,
    title = title,
    notes = description,
    dateTime = dateTime?.toDate()?.time ?: System.currentTimeMillis(),
    mainCategory = category,
    isCompleted = isCompleted,
    isDeleted = isDeleted,
    createdAt = createdAt.toDate().time,
    updatedAt = updatedAt.toDate().time
)

fun Category.toFirestore(userId: String) = FirestoreCategory(
    id = id.toString(),
    name = name,
    icon = iconName,
    userId = userId,
    createdAt = Timestamp.now()
)

fun FirestoreCategory.toRoom() = Category(
    id = id.toLongOrNull() ?: 0L,
    name = name,
    iconName = icon,
    isMainCategory = true,
    isPreset = false
)

fun Template.toFirestore(userId: String) = FirestoreTemplate(
    id = id.toString(),
    title = title,
    description = notes,
    category = mainCategory,
    userId = userId,
    createdAt = Timestamp.now()
)

fun FirestoreTemplate.toRoom() = Template(
    id = id.toLongOrNull() ?: 0L,
    name = title,
    title = title,
    notes = description,
    mainCategory = category,
    recurrenceType = "ONE_TIME",
    recurrenceInterval = 1
)