package com.example.farmforward.appActivity.userActivity.login

import android.content.Context
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

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
            // 1. Offline Check
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
                    val query = if (isInputEmail) usersRef.whereEqualTo("email", identifier) else usersRef.whereEqualTo("username", identifier)
                    val snapshot = query.get().await()
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        emailToUse = doc.getString("email")
                        localUser = User(
                            id = doc.getLong("id")?.toInt() ?: 0,
                            username = doc.getString("username") ?: "",
                            email = emailToUse ?: "",
                            password = password
                        )
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            if (emailToUse.isNullOrEmpty()) {
                withContext(Dispatchers.Main) { view?.showToast("Account not found.", isError = true) }
                return@launch
            }

            auth.signInWithEmailAndPassword(emailToUse!!, password)
                .addOnSuccessListener { authResult ->
                    ioScope.launch {

                        if (localUser == null) {
                            val firebaseUser = authResult.user
                            if (firebaseUser != null) {
                                localUser = User(
                                    id = 0,
                                    username = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Farmer",
                                    email = firebaseUser.email ?: emailToUse!!,
                                    password = password
                                )
                            }
                        }

                        if (localUser != null) {
                            val updatedUser = localUser!!.copy(password = password)

                            userDao.registerUser(updatedUser)

                            session.saveSession(updatedUser.id, updatedUser.username, updatedUser.email)
                            session.saveOfflineMode(isOfflineMode)

                            withContext(Dispatchers.Main) {
                                view?.showToast("Login successful!", isError = false)
                                view?.startLoginSync()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                view?.showToast("Error: Could not save user data locally.", isError = true)
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
            var user: User? = null
            val isInputEmail = identifier.contains("@")
            if (isInputEmail) {
                user = userDao.getUserByEmail(identifier)
            } else {
                user = userDao.getUserByUsername(identifier)
            }

            withContext(Dispatchers.Main) {
                if (user != null && user!!.password == password) {
                    session.saveSession(user!!.id, user!!.username, user!!.email)
                    session.saveOfflineMode(isOffline)
                    view?.showToast("Offline Login Successful!", isError = false)
                    view?.startLoginSync()
                } else {
                    if (user == null) {
                        view?.showToast("User not found on this device. Login Online first.", isError = true)
                    } else {
                        view?.showToast("Incorrect Password for Offline Mode.", isError = true)
                    }
                }
            }
        }
    }
}