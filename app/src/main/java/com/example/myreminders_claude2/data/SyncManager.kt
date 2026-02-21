package com.example.myreminders_claude2.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
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
            // Upload local data first, then start listeners
            syncLocalToCloud()
            Log.d(TAG, "Local upload complete - now starting listeners")
            setupRealtimeListeners(uid)
            // Only fetch from cloud on genuine fresh install (empty local DB)
            val localCount = reminderDao.getAllActiveRemindersSync().size
            if (localCount == 0) {
                Log.d(TAG, "Empty local DB - fetching from cloud for fresh install restore")
                fetchAllFromCloud(uid)
            } else {
                Log.d(TAG, "Local DB has $localCount reminders - skipping cloud fetch")
            }
        }
    }

    private suspend fun fetchAllFromCloud(uid: String) {
        try {
            Log.d(TAG, "Fetching all reminders from cloud for fresh install/restore")
            val snapshot = firestore.collection(COLLECTION_REMINDERS)
                .whereEqualTo("userId", uid)
                .get().await()
            snapshot.documents.forEach { doc ->
                val firestoreReminder = doc.toObject(FirestoreReminder::class.java) ?: return@forEach
                handleReminderChange(firestoreReminder)
            }
            Log.d(TAG, "Cloud fetch complete: ${snapshot.size()} reminders")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching from cloud", e)
        }
    }

    fun syncNow() {
        scope.launch {
            syncLocalToCloud()
            Log.d(TAG, "Foreground sync complete")
        }
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
        // ✅ FIXED: Now handles ADDED, MODIFIED, and REMOVED separately
        remindersListener = firestore.collection(COLLECTION_REMINDERS)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Reminders listener error", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    scope.launch {
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                // Sync from cloud to Room
                                val firestoreReminder = change.document.toObject(FirestoreReminder::class.java)
                                handleReminderChange(firestoreReminder)
                            }
                            DocumentChange.Type.REMOVED -> {
                                // Do NOT delete from Room on cloud removal events.
                                // Firestore fires REMOVED when permissions are denied or
                                // the listener loses access — deleting locally would wipe
                                // the user's data. Local deletion is always user-initiated.
                                Log.d(TAG, "Ignoring cloud REMOVED event for reminder: ${change.document.id}")
                            }
                        }
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

        // ✅ Check permanently deleted table first
        val isPermanentlyDeleted = reminderDao.isPermanentlyDeleted(localReminder.id)
        if (isPermanentlyDeleted > 0) {
            Log.d(TAG, "Skipping permanently deleted reminder: ${localReminder.title}")
            // Clean up Firebase too
            deleteReminderFromCloud(localReminder.id)
            return
        }

        val existing = reminderDao.getReminderByIdSync(localReminder.id)

        if (existing == null) {
            // Restore to Room regardless of isDeleted/isCompleted status
            // This ensures Done and Missed reminders are restored on fresh install
            reminderDao.insertReminder(localReminder)
            Log.d(TAG, "Downloaded reminder: ${localReminder.title} (isDeleted=${localReminder.isDeleted}, isCompleted=${localReminder.isCompleted})")
        } else {
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

fun Reminder.toFirestore(userId: String) = FirestoreReminder(
    id = id.toString(),
    title = title,
    description = notes,
    dateTime = Timestamp(Date(dateTime)),
    category = mainCategory,
    completed = isCompleted,
    deleted = isDeleted,
    createdAt = Timestamp(Date(createdAt)),
    updatedAt = Timestamp(Date(updatedAt)),
    userId = userId,
    // ✅ All missing fields now saved:
    completedAt = completedAt?.let { Timestamp(Date(it)) },
    deletedAt = deletedAt?.let { Timestamp(Date(it)) },
    dismissalReason = dismissalReason,
    recurrenceType = recurrenceType,
    recurrenceInterval = recurrenceInterval,
    recurrenceDayOfWeek = recurrenceDayOfWeek,
    recurrenceDayOfMonth = recurrenceDayOfMonth,
    recurringGroupId = recurringGroupId,
    subCategory = subCategory,
    isVoiceEnabled = isVoiceEnabled,
    snoozeCount = snoozeCount
)

fun FirestoreReminder.toRoom() = Reminder(
    id = id.toLongOrNull() ?: 0L,
    title = title,
    notes = description,
    dateTime = dateTime?.toDate()?.time ?: System.currentTimeMillis(),
    mainCategory = category,
    isCompleted = completed,
    isDeleted = deleted,
    createdAt = createdAt.toDate().time,
    updatedAt = updatedAt.toDate().time,
    // ✅ All missing fields now restored:
    completedAt = completedAt?.toDate()?.time,
    deletedAt = deletedAt?.toDate()?.time,
    dismissalReason = dismissalReason,
    recurrenceType = recurrenceType,
    recurrenceInterval = recurrenceInterval,
    recurrenceDayOfWeek = recurrenceDayOfWeek,
    recurrenceDayOfMonth = recurrenceDayOfMonth,
    recurringGroupId = recurringGroupId,
    subCategory = subCategory,
    isVoiceEnabled = isVoiceEnabled,
    snoozeCount = snoozeCount
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