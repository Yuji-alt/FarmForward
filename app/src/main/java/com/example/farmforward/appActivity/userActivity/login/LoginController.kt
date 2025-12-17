package com.example.farmforward.appActivity.userActivity.login

import android.content.Context
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.otherUtils.HashUtils
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
        updateNetworkState(NetworkUtils.isNetworkAvailable(context))
    }

    fun onNetworkChanged(isAvailable: Boolean) {
        updateNetworkState(isAvailable)
    }

    private fun updateNetworkState(isAvailable: Boolean) {
        if (!isAvailable) {
            view?.setOfflineSwitch(true)
            view?.enableSignUpButton(false)
        } else {
            view?.setOfflineSwitch(false)
            view?.enableSignUpButton(true)
        }
    }

    fun onDestroy() {
        ioScope.cancel()
        view = null
    }


    fun onSignUpClicked() {
        view?.navigateToSignUp()
    }

    fun onForgotPasswordClicked() {
        view?.showForgotPasswordDialog()
    }

    fun onLoginClicked(identifier: String, password: String, isOffline: Boolean) {
        if (identifier.isEmpty() || password.isEmpty()) {
            view?.showToast("Please fill in all fields", isError = true)
            return
        }
        handleLogin(identifier, password, isOffline)
    }

    private fun handleLogin(identifier: String, password: String, isOfflineMode: Boolean) {
        val useOffline = isOfflineMode

        ioScope.launch {
            if (useOffline || !NetworkUtils.isNetworkAvailable(context)) {
                checkLocal(identifier, password, useOffline)
                return@launch
            }

            var currentUser: User? = null
            var emailToUse: String? = null
            val isInputEmail = identifier.contains("@")

            if (isInputEmail) {
                currentUser = userDao.getUserByEmail(identifier)
                emailToUse = identifier
            } else {
                currentUser = userDao.getUserByUsername(identifier)
                emailToUse = currentUser?.email
            }

            if (currentUser == null) {
                try {
                    val usersRef = firestore.collection("users")
                    val query = if (isInputEmail)
                        usersRef.whereEqualTo("email", identifier)
                    else
                        usersRef.whereEqualTo("username", identifier)

                    val snapshot = query.get().await()
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        emailToUse = doc.getString("email")
                        val prettyName = doc.getString("username") ?: doc.id

                        currentUser = User(
                            id = 0,
                            username = prettyName,
                            email = emailToUse ?: "",
                            password = ""
                        )
                    }
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                try {
                    val usersRef = firestore.collection("users")
                    val query = if (isInputEmail)
                        usersRef.whereEqualTo("email", identifier)
                    else
                        usersRef.whereEqualTo("username", identifier)

                    val snapshot = query.get().await()
                    if (!snapshot.isEmpty) {
                        val doc = snapshot.documents[0]
                        val prettyName = doc.getString("username") ?: doc.id
                        if (currentUser?.username != prettyName) {
                            currentUser = currentUser?.copy(username = prettyName)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            if (emailToUse.isNullOrEmpty()) {
                withContext(Dispatchers.Main) { view?.showToast("Account not found.", isError = true) }
                return@launch
            }

            auth.signInWithEmailAndPassword(emailToUse!!, password)
                .addOnSuccessListener { authResult ->
                    val firebaseUser = authResult.user

                    if (firebaseUser != null && !firebaseUser.isEmailVerified) {
                        auth.signOut()
                        ioScope.launch(Dispatchers.Main) {
                            view?.showUnverifiedAccountDialog(emailToUse!!, password)
                        }
                        return@addOnSuccessListener
                    }

                    ioScope.launch {
                        if (currentUser == null && firebaseUser != null) {
                            currentUser = User(
                                id = 0,
                                username = firebaseUser.displayName ?: "Farmer",
                                email = firebaseUser.email ?: emailToUse!!,
                                password = ""
                            )
                        }

                        currentUser?.let { userToSave ->
                            val hashedPassword = HashUtils.hashPassword(password)
                            val updatedUser = userToSave.copy(password = hashedPassword)

                            userDao.registerUser(updatedUser)
                            val finalUser = userDao.getUserByEmail(updatedUser.email) ?: updatedUser

                            session.saveSession(finalUser.id, finalUser.username, finalUser.email)
                            session.saveOfflineMode(useOffline)

                            withContext(Dispatchers.Main) {
                                view?.showToast("Login successful!", isError = false)
                                view?.startLoginSync()
                            }
                        } ?: run {
                            withContext(Dispatchers.Main) {
                                view?.showToast("Error: User data is missing.", isError = true)
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

    private fun checkLocal(identifier: String, password: String, isOffline: Boolean) {
        ioScope.launch {
            var user: User? = null
            val isInputEmail = identifier.contains("@")
            if (isInputEmail) {
                user = userDao.getUserByEmail(identifier)
            } else {
                user = userDao.getUserByUsername(identifier)
            }
            val inputHash = HashUtils.hashPassword(password)

            withContext(Dispatchers.Main) {
                if (user != null && user!!.password == inputHash) {
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
    fun resendVerificationEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                result.user?.sendEmailVerification()
                    ?.addOnSuccessListener {
                        view?.showToast("Verification email sent! Check your inbox or Spam.", isError = false)
                        auth.signOut()
                    }
                    ?.addOnFailureListener { e ->
                        view?.showToast("Failed to send: ${e.message}", isError = true)
                        auth.signOut()
                    }
            }
            .addOnFailureListener {
                view?.showToast("Could not access account to send email.", isError = true)
            }
    }

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
}