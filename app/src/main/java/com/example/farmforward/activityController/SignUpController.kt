package com.example.farmforward.activityController

import android.content.Context
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.roomDatabase.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpController(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    fun signUp(username: String, password: String, confirm: String, onResult: (Boolean, String) -> Unit) {
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            onResult(false, "Please fill in all fields")
            return
        }

        if (password != confirm) {
            onResult(false, "Passwords do not match")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val exists = db.userDao().checkUserExists(username)
            if (exists > 0) {
                onResult(false, "Username already exists!")
            } else {
                val user = User(username = username, password = password)
                db.userDao().registerUser(user)
                onResult(true, "Registration successful!")
            }
        }
    }
}
