package com.example.farmforward.activityController

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.example.farmforward.activityViewmodel.SignUpActivity
import com.example.farmforward.roomDatabase.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginController(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)

    fun login(username: String, password: String, onResult: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val user = db.userDao().loginUser(username, password)
            onResult(user != null)
        }
    }
}
