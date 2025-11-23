package com.example.farmforward.database.firebaseDatabase

import com.example.farmforward.database.roomDatabase.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore // Hilt injects this singleton
) {

    private val usersRef = firestore.collection("users")

    suspend fun registerUser(user: User) {
        usersRef.document(user.username).set(user).await()
    }

    suspend fun checkUserExists(username: String): Boolean {
        val doc = usersRef.document(username).get().await()
        return doc.exists()
    }

    suspend fun loginUser(username: String, password: String): User? {
        val doc = usersRef.document(username).get().await()
        val user = doc.toObject(User::class.java)
        return if (user != null && user.password == password) user else null
    }

    suspend fun getUserByUsername(username: String): User? {
        val doc = usersRef.document(username).get().await()
        return doc.toObject(User::class.java)
    }
}