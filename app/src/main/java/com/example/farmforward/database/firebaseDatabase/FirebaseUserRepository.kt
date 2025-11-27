package com.example.farmforward.database.firebaseDatabase

import com.example.farmforward.database.roomDatabase.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val usersRef = firestore.collection("users")

    suspend fun registerUser(user: User) {

        val safeUserMap = hashMapOf(
            "id" to user.id,
            "username" to user.username,
            "email" to user.email,
            "lastUpdated" to user.lastUpdated
        )

        usersRef.document(user.username).set(safeUserMap).await()
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