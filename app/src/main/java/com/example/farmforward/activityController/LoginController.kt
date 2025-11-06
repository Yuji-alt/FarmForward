package com.example.farmforward.activityController

import android.content.Context
import com.example.farmforward.firebase.FirebaseSyncManager
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.roomDatabase.User
import com.example.farmforward.session.SessionManager
import com.example.farmforward.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class LoginController(private val context: Context) {

    private val localDb = AppDatabase.getDatabase(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestore = FirebaseFirestore.getInstance()

    fun login(username: String, password: String, onResult: (User?) -> Unit) {
        ioScope.launch {
            if (NetworkUtils.isNetworkAvailable(context)) {
                firestore.collection("users")
                    .whereEqualTo("username", username)
                    .whereEqualTo("password", password)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            val doc = result.documents[0]
                            val user = User(
                                id = doc.getLong("id")?.toInt() ?: 0,
                                username = doc.getString("username") ?: "",
                                password = doc.getString("password") ?: ""
                            )

                            ioScope.launch {

                                localDb.userDao().registerUser(user)
                                val session = SessionManager(context)
                                session.saveSession(user.id, user.username)
                                val syncManager = FirebaseSyncManager(context)
                                syncManager.syncUsers()
                                syncManager.syncCrops()
                            }

                            onResult(user)
                        } else {
                            checkLocal(username, password, onResult)
                        }
                    }
                    .addOnFailureListener {
                        checkLocal(username, password, onResult)
                    }
            } else {
                checkLocal(username, password, onResult)
            }
        }
    }

    private fun checkLocal(username: String, password: String, onResult: (User?) -> Unit) {
        ioScope.launch {
            val user = localDb.userDao().loginUser(username, password)
            user?.let {
                val session = SessionManager(context)
                session.saveSession(it.id, it.username)
            }
            withContext(Dispatchers.Main) {
                onResult(user)
            }
        }
    }
}
