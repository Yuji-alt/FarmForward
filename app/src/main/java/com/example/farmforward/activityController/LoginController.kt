package com.example.farmforward.activityController

import android.content.Context
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.roomDatabase.User
import kotlinx.coroutines.*

class LoginController(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun login(username: String, password: String, onResult: (User?) -> Unit) {
        ioScope.launch {
            val user = db.userDao().loginUser(username, password)

            withContext(Dispatchers.Main) {
                onResult(user)
            }
        }
    }
}
