package com.example.farmforward.firebaseDatabase

import com.example.farmforward.roomDatabase.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseUserRepository {

    val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")

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
