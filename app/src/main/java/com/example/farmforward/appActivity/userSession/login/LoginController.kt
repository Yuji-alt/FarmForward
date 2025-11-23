package com.example.farmforward.appActivity.userSession.login

import android.content.Context
import android.widget.Toast
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.appActivity.userSession.session.SessionManager
import com.example.farmforward.utils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LoginController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: RoomUserDao,
    private val session: SessionManager,
    private val firestore: FirebaseFirestore
) {

    private var view: LoginView? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun bindView(view: LoginView) {
        this.view = view
    }

    fun onViewCreated() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            view?.setOfflineSwitch(true)
            view?.enableSignUpButton(false)
            view?.showToast("No network. Offline mode is ON.", Toast.LENGTH_LONG)
        } else {
            view?.setOfflineSwitch(false)
            view?.enableSignUpButton(true)
        }
    }

    fun onSignUpClicked() {
        view?.navigateToSignUp()
    }

    fun onLoginClicked(username: String, password: String, isOffline: Boolean) {
        if (username.isEmpty() || password.isEmpty()) {
            view?.showToast("Please fill in all fields")
            return
        }

        handleLogin(username, password, isOffline)
    }

    fun onDestroy() {
        ioScope.cancel()
        view = null
    }

    private fun handleLogin(username: String, password: String, isOfflineMode: Boolean) {
        ioScope.launch {
            if (NetworkUtils.isNetworkAvailable(context) && !isOfflineMode) {
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
                                userDao.registerUser(user)
                                session.saveSession(user.id, user.username)

                                withContext(Dispatchers.Main) {
                                    view?.showToast("Login successful!")
                                    // CHANGED: Trigger the DialogSync in Activity
                                    view?.startLoginSync()
                                }
                            }
                        } else {
                            checkLocal(username, password, isOfflineMode)
                        }
                    }
                    .addOnFailureListener {
                        checkLocal(username, password, isOfflineMode)
                    }
            } else {
                checkLocal(username, password, isOfflineMode)
            }
        }
    }

    private fun checkLocal(username: String, password: String, isOffline: Boolean) {
        ioScope.launch {
            val user = userDao.loginUser(username, password)

            withContext(Dispatchers.Main) {
                if (user != null) {
                    session.saveSession(user.id, user.username)
                    view?.showToast("Login successful!")
                    // CHANGED: Trigger the DialogSync in Activity
                    view?.startLoginSync()
                } else {
                    val message = if (isOffline) {
                        "Invalid credentials for offline mode."
                    } else {
                        "Invalid credentials or no network."
                    }
                    view?.showToast(message)
                }
            }
        }
    }
}