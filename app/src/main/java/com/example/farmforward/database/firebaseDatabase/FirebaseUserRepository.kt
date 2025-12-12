package com.example.farmforward.database.firebaseDatabase

import com.example.farmforward.database.roomDatabase.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val usersRef = firestore.collection("users")

    suspend fun registerUser(user: User) {
        val authResult = auth.createUserWithEmailAndPassword(user.email, user.password).await()
        val firebaseUid = authResult.user?.uid ?: throw Exception("Auth failed")

        val publicProfileMap = hashMapOf(
            "id" to user.id,
            "firebaseUid" to firebaseUid,
            "username" to user.username,
            "email" to user.email,
            "lastUpdated" to user.lastUpdated
        )
        usersRef.document(user.username.lowercase()).set(publicProfileMap).await()
    }

    suspend fun loginUser(usernameOrEmail: String, password: String): User? {
        var emailToLogin = usernameOrEmail
        if (!usernameOrEmail.contains("@")) {
            val snapshot = usersRef.whereEqualTo("username", usernameOrEmail).get().await()
            if (!snapshot.isEmpty) {
                emailToLogin = snapshot.documents[0].getString("email") ?: return null
            } else {
                return null
            }
        }
        return try {
            auth.signInWithEmailAndPassword(emailToLogin, password).await()
            val query = usersRef.whereEqualTo("email", emailToLogin).get().await()
            if (!query.isEmpty) query.documents[0].toObject(User::class.java) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updatePassword(email: String, oldPass: String, newPass: String) {
        val user = auth.currentUser
        if (user != null && user.email == email) {
            user.updatePassword(newPass).await()
        } else {
            throw Exception("User not logged in or email mismatch")
        }
    }

    // --- FIX: Delete Crops Subcollection Manually ---
    suspend fun updateUsername(oldDocId: String, user: User, newUsername: String) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        // 1. DELETE OLD CROPS (Essential for cleaning up the old name)
        try {
            val oldCropsRef = usersRef.document(oldDocId).collection("crops")
            val snapshot = oldCropsRef.get().await()
            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. MIGRATE USER DOC
        val userMap = hashMapOf(
            "id" to user.id,
            "username" to newUsername,
            "email" to user.email,
            "lastUpdated" to System.currentTimeMillis(),
            "firebaseUid" to uid
        )

        firestore.runTransaction { transaction ->
            val oldDocRef = usersRef.document(oldDocId)
            val newDocRef = usersRef.document(newUsername.lowercase())

            transaction.set(newDocRef, userMap)

            if (oldDocId != newUsername.lowercase()) {
                transaction.delete(oldDocRef)
            }
        }.await()
    }

    suspend fun deleteUser(username: String) {
        try {
            // 1. Delete Crops First
            val cropsRef = usersRef.document(username.lowercase()).collection("crops")
            val snapshot = cropsRef.get().await()
            if (!snapshot.isEmpty) {
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }
            // 2. Delete User Doc
            usersRef.document(username.lowercase()).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val user = auth.currentUser
        user?.delete()?.await()
    }
}