package com.example.farmforward.appActivity.userActivity.login

import android.content.Context
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.tasks.await

class LoginController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: RoomUserDao,
    private val session: SessionManager,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
            view?.showToast("No network. Offline mode is ON.", isError = true)
        } else {
            view?.setOfflineSwitch(false)
            view?.enableSignUpButton(true)
        }
    }
    fun onSignUpClicked() {
        view?.navigateToSignUp()
    }
    fun onLoginClicked(identifier: String, password: String, isOffline: Boolean) {
        if (identifier.isEmpty() || password.isEmpty()) {
            view?.showToast("Please fill in all fields", isError = true)
            return
        }
        handleLogin(identifier, password, isOffline)
    }
    fun onDestroy() {
        ioScope.cancel()
        view = null
    }
    private fun handleLogin(identifier: String, password: String, isOfflineMode: Boolean) {
        ioScope.launch {
            if (isOfflineMode || !NetworkUtils.isNetworkAvailable(context)) {
                checkLocal(identifier, password, isOfflineMode)
                return@launch
            }

            var emailToUse: String? = null
            var localUser: User? = null
            val isInputEmail = identifier.contains("@")

            if (isInputEmail) {
                localUser = userDao.getUserByEmail(identifier)
                emailToUse = identifier
            } else {
                localUser = userDao.getUserByUsername(identifier)
                emailToUse = localUser?.email
            }
            if (localUser == null) {
                try {
                    val usersRef = firestore.collection("users")
                    val query = if (isInputEmail) {
                        usersRef.whereEqualTo("email", identifier)
                    } else {
                        usersRef.whereEqualTo("username", identifier)
                    }
                    val snapshot = query.get().await()
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        val dbEmail = doc.getString("email")
                        emailToUse = dbEmail
                        localUser = User(
                            id = doc.getLong("id")?.toInt() ?: 0,
                            username = doc.getString("username") ?: "",
                            email = dbEmail ?: "",
                            password = password
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (emailToUse.isNullOrEmpty()) {
                withContext(Dispatchers.Main) {
                    view?.showToast("Account not found.", isError = true)
                }
                return@launch
            }

            auth.signInWithEmailAndPassword(emailToUse!!, password)
                .addOnSuccessListener {
                    ioScope.launch {
                        if (localUser != null) {
                            val updatedUser = localUser!!.copy(password = password)
                            userDao.registerUser(updatedUser)
                            session.saveSession(updatedUser.id, updatedUser.username)

                            withContext(Dispatchers.Main) {
                                view?.showToast("Login successful!", isError = false)
                                view?.startLoginSync()
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    val msg = if (e is FirebaseAuthInvalidCredentialsException) "Incorrect password." else e.message ?: "Login failed"
                    view?.showToast(msg, isError = true)
                }
        }
    }
    fun onForgotPasswordClicked() { view?.showForgotPasswordDialog() }

    fun sendPasswordReset(email: String) {
        if (email.isEmpty()) {
            view?.showToast("Please enter your email", isError = true)
            return
        }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                view?.showToast("If that email is registered, we sent a link.", isError = false)
            }
            .addOnFailureListener { e ->
                view?.showToast(e.localizedMessage ?: "Failed to send reset email", isError = true)
            }
    }

    private fun checkLocal(identifier: String, password: String, isOffline: Boolean) {
        ioScope.launch {
            val user = userDao.loginUser(identifier, password)

            withContext(Dispatchers.Main) {
                if (user != null) {
                    session.saveSession(user.id, user.username)
                    view?.startLoginSync()
                } else {
                    val message = if (isOffline) {
                        "Invalid credentials for offline mode."
                    } else {
                        "Invalid credentials or no network."
                    }
                    view?.showToast(message, isError = true)
                }
            }
        }
    }
}