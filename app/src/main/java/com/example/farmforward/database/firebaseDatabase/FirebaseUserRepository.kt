package com.example.farmforward.database.firebaseDatabase

import com.example.farmforward.database.roomDatabase.User
import com.google.firebase.auth.EmailAuthProvider
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

        usersRef.document(user.username).set(publicProfileMap).await()
    }

    suspend fun loginUser(usernameOrEmail: String, password: String): User? {
        var emailToLogin = usernameOrEmail

        if (!usernameOrEmail.contains("@")) {
            val snapshot = usersRef.document(usernameOrEmail).get().await()
            if (snapshot.exists()) {
                emailToLogin = snapshot.getString("email") ?: return null
            } else {
                return null
            }
        }

        return try {
            auth.signInWithEmailAndPassword(emailToLogin, password).await()

            val query = usersRef.whereEqualTo("email", emailToLogin).get().await()
            if (!query.isEmpty) {
                query.documents[0].toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun updatePassword(email: String, oldPass: String, newPass: String) {
        val user = auth.currentUser

        if (user != null && user.email == email) {
            val credential = EmailAuthProvider.getCredential(email, oldPass)

            user.reauthenticate(credential).await()

            user.updatePassword(newPass).await()
        } else {
            throw Exception("User not logged in or email mismatch")
        }
    }

    suspend fun updateUsername(oldUsername: String, user: User) {
        val userMap = hashMapOf(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "lastUpdated" to System.currentTimeMillis()
        )

        firestore.runTransaction { transaction ->
            val oldDocRef = usersRef.document(oldUsername)
            val newDocRef = usersRef.document(user.username)
            transaction.set(newDocRef, userMap)
            transaction.delete(oldDocRef)
        }.await()
    }

    suspend fun deleteUser(username: String) {
        try {
            usersRef.document(username).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val user = auth.currentUser
        user?.delete()?.await()
    }
}