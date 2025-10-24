package com.example.farmforward.activityController

import android.content.Context
import com.example.farmforward.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.roomDatabase.User
import kotlinx.coroutines.*

class SignUpController(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun signUp(username: String, password: String, confirm: String, onResult: (Boolean, String) -> Unit) {
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirm.trim()

        if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty() || trimmedConfirm.isEmpty()) {
            onResult(false, "Please fill in all fields")
            return
        }

        if (trimmedPassword.length < 6) {
            onResult(false, "Password must be at least 6 characters")
            return
        }


        if (trimmedPassword != trimmedConfirm) {
            onResult(false, "Passwords do not match")
            return
        }

        ioScope.launch {
            val exists = db.userDao().checkUserExists(trimmedUsername)
            if (exists > 0) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Username already exists!")
                }
            } else {
                val user = User(username = trimmedUsername, password = trimmedPassword, lastUpdated = System.currentTimeMillis())
                db.userDao().registerUser(user)
                FirebaseUserRepository().registerUser(user)
                withContext(Dispatchers.Main) {
                    onResult(true, "Registration successful!")
                }
            }
        }
    }
}
