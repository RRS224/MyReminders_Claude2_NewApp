package com.example.myreminders_claude2.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Manages sharing codes and My Circle connections in Firestore.
 *
 * Firestore structure:
 *
 * users/{uid}/
 *     sharingCode: "RAM447"
 *     displayName: "Ranjit"
 *     fcmToken: "..."
 *     createdAt: timestamp
 *
 * sharingCodes/{code}/
 *     uid: "firebase-uid"
 *     createdAt: timestamp
 *
 * circles/{uid}/connections/{connectionUid}/
 *     displayName: "Dad"
 *     sharingCode: "DAD123"
 *     addedAt: timestamp
 */
object CircleManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val TAG = "CircleManager"

    // ── Sharing Code ──────────────────────────────────────────────────────────

    /**
     * Gets the current user's sharing code.
     * Creates one if it doesn't exist yet.
     */
    suspend fun getOrCreateSharingCode(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            if (userDoc.exists() && userDoc.getString("sharingCode") != null) {
                userDoc.getString("sharingCode")
            } else {
                createNewSharingCode(uid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sharing code", e)
            null
        }
    }

    /**
     * Generates a new sharing code and saves it to Firestore.
     * Invalidates the old code in the sharingCodes lookup table.
     */
    suspend fun regenerateSharingCode(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            // Remove old code from lookup table
            val userDoc = firestore.collection("users").document(uid).get().await()
            val oldCode = userDoc.getString("sharingCode")
            if (oldCode != null) {
                firestore.collection("sharingCodes").document(oldCode).delete().await()
            }
            createNewSharingCode(uid)
        } catch (e: Exception) {
            Log.e(TAG, "Error regenerating sharing code", e)
            null
        }
    }

    private suspend fun createNewSharingCode(uid: String): String {
        val code = generateCode()

        // Save to user profile
        val displayName = auth.currentUser?.displayName
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "User"

        firestore.collection("users").document(uid).set(
            mapOf(
                "sharingCode" to code,
                "displayName" to displayName,
                "uid" to uid,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        ).await()

        // Save to lookup table (code → uid)
        firestore.collection("sharingCodes").document(code).set(
            mapOf(
                "uid" to uid,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
        ).await()

        Log.d(TAG, "Created sharing code: $code for uid: $uid")
        return code
    }

    /**
     * Generates a readable 6-character code: 3 uppercase letters + 3 digits.
     * e.g. RAM447, DAD123, JOY886
     * Avoids ambiguous characters: 0/O, 1/I/L
     */
    private fun generateCode(): String {
        val letters = "ABCDEFGHJKMNPQRSTUVWXYZ" // no I, L, O
        val digits = "23456789"                   // no 0, 1
        val letters3 = (1..3).map { letters.random() }.joinToString("")
        val digits3 = (1..3).map { digits.random() }.joinToString("")
        return "$letters3$digits3"
    }

    // ── User lookup ───────────────────────────────────────────────────────────

    /**
     * Looks up a user by their sharing code.
     * Returns their uid and displayName, or null if not found.
     */
    suspend fun findUserByCode(code: String): Map<String, String>? {
        return try {
            val codeDoc = firestore.collection("sharingCodes")
                .document(code.uppercase().trim())
                .get().await()

            if (!codeDoc.exists()) return null

            val uid = codeDoc.getString("uid") ?: return null
            val userDoc = firestore.collection("users").document(uid).get().await()
            val displayName = userDoc.getString("displayName") ?: "Unknown"

            mapOf("uid" to uid, "displayName" to displayName)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by code", e)
            null
        }
    }

    // ── Connections ───────────────────────────────────────────────────────────

    /**
     * Adds a connection to the current user's circle.
     */
    suspend fun addConnection(
        connectionUid: String,
        displayName: String,
        sharingCode: String
    ): Boolean {
        val myUid = auth.currentUser?.uid ?: return false
        return try {
            firestore
                .collection("circles")
                .document(myUid)
                .collection("connections")
                .document(connectionUid)
                .set(mapOf(
                    "uid" to connectionUid,
                    "displayName" to displayName,
                    "sharingCode" to sharingCode,
                    "addedAt" to com.google.firebase.Timestamp.now()
                )).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding connection", e)
            false
        }
    }

    /**
     * Gets all connections for the current user.
     */
    suspend fun getConnections(): List<Map<String, String>> {
        val myUid = auth.currentUser?.uid ?: return emptyList()
        return try {
            firestore
                .collection("circles")
                .document(myUid)
                .collection("connections")
                .get().await()
                .documents
                .map { doc ->
                    mapOf(
                        "uid" to (doc.getString("uid") ?: ""),
                        "displayName" to (doc.getString("displayName") ?: "Unknown"),
                        "sharingCode" to (doc.getString("sharingCode") ?: "")
                    )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connections", e)
            emptyList()
        }
    }
}
