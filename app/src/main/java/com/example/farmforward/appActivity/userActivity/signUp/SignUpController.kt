package com.example.farmforward.appActivity.userActivity.signUp

import android.content.Context
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // Important for Firestore check
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SignUpController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: RoomUserDao,
    private val firebaseRepo: FirebaseUserRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private var view: SignUpView? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun bindView(view: SignUpView) {
        this.view = view
    }

    fun onBackClicked() {
        view?.navigateToLogin()
    }

    fun onSignUpClicked(email: String, username: String, password: String, confirm: String) {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirm.trim()

        if (trimmedEmail.isEmpty() || trimmedUsername.isEmpty() || trimmedPassword.isEmpty() || trimmedConfirm.isEmpty()) {
            view?.showToast("Please fill in all fields", isError = true)
            return
        }
        if (trimmedPassword.length < 6) {
            view?.showToast("Password must be at least 6 characters", isError = true)
            return
        }
        if (trimmedPassword != trimmedConfirm) {
            view?.showToast("Passwords do not match", isError = true)
            return
        }

        view?.setSignUpButtonEnabled(false)

        ioScope.launch {
            val localExists = userDao.checkUserExists(trimmedUsername)
            if (localExists > 0) {
                withContext(Dispatchers.Main) {
                    view?.showToast("Username already exists on this device!", isError = true)
                    view?.setSignUpButtonEnabled(true)
                }
                return@launch
            }

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    val snapshot = firestore.collection("users")
                        .whereEqualTo("username", trimmedUsername)
                        .get()
                        .await()

                    if (!snapshot.isEmpty) {
                        withContext(Dispatchers.Main) {
                            view?.showToast("Username is already taken by another user.", isError = true)
                            view?.setSignUpButtonEnabled(true)
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        view?.showToast("Network error checking username. Please try again.", isError = true)
                        view?.setSignUpButtonEnabled(true)
                    }
                    return@launch
                }
            }
            val newUser = User(
                username = trimmedUsername,
                password = trimmedPassword,
                email = trimmedEmail,
                lastUpdated = System.currentTimeMillis()
            )

            if (NetworkUtils.isNetworkAvailable(context)) {
                try {
                    auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                        .addOnSuccessListener { authResult ->
                            ioScope.launch {
                                saveUserToDatabases(newUser)
                            }
                        }
                        .addOnFailureListener { e ->
                            view?.setSignUpButtonEnabled(true)
                            view?.showToast("Registration Failed: ${e.message}", isError = true)
                        }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        view?.setSignUpButtonEnabled(true)
                        view?.showToast("Error: ${e.message}", isError = true)
                    }
                }
            } else {
                saveUserToDatabases(newUser)
                withContext(Dispatchers.Main) {
                    view?.showToast("Saved Offline. Please sync later.", isError = false)
                }
            }
        }
    }

    private suspend fun saveUserToDatabases(user: User) {
        userDao.registerUser(user)
        if (NetworkUtils.isNetworkAvailable(context)) {
            firebaseRepo.registerUser(user)
        }
        withContext(Dispatchers.Main) {
            view?.showToast("Registration successful!", isError = false)
            view?.navigateToLogin()
        }
    }

    fun onDestroy() {
        view = null
        ioScope.cancel()
    }
}