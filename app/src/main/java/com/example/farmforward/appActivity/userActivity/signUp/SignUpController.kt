package com.example.farmforward.appActivity.userActivity.signUp

import android.content.Context
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.otherUtils.HashUtils
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SignUpController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: RoomUserDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private var view: SignUpView? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun bindView(view: SignUpView) {
        this.view = view
    }

    fun onDestroy() {
        view = null
        ioScope.cancel()
    }

    fun onBackClicked() {
        view?.navigateToLogin()
    }

    fun onSignUpClicked(email: String, username: String, password: String, confirm: String) {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirm.trim()
        val usernameId = trimmedUsername.lowercase()

        // 1. Basic Empty Checks
        if (trimmedEmail.isEmpty() || trimmedUsername.isEmpty() || trimmedPassword.isEmpty() || trimmedConfirm.isEmpty()) {
            view?.showToast("Please fill in all fields", isError = true)
            return
        }

        if (trimmedPassword != trimmedConfirm) {
            view?.showToast("Passwords do not match", isError = true)
            return
        }

        if (trimmedPassword.length < 6) {
            view?.showToast("Password must be at least 6 characters", isError = true)
            return
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            view?.showToast("Internet connection is required to sign up.", isError = true)
            return
        }

        view?.setSignUpButtonEnabled(false)
        view?.showLoading()

        ioScope.launch {
            fun update(progress: Int, message: String) {
                view?.updateLoading(progress, message)
            }

            update(10, "Checking email availability...")
            delay(300)

            try {
                val userDoc = firestore.collection("users").document(usernameId).get().await()
                if (userDoc.exists()) {
                    withContext(Dispatchers.Main) {
                        view?.hideLoading()
                        view?.showToast("Username '$trimmedUsername' is already taken.", isError = true)
                        view?.setSignUpButtonEnabled(true)
                    }
                    return@launch
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view?.hideLoading()
                    view?.showToast("Network error checking email: ${e.message}", isError = true)
                    view?.setSignUpButtonEnabled(true)
                }
                return@launch
            }

            update(30, "Checking username availability...")

            // Local Check
            val localExists = userDao.checkUserExists(trimmedUsername)
            if (localExists > 0) {
                withContext(Dispatchers.Main) {
                    view?.hideLoading()
                    view?.showToast("Username exists on this device!", isError = true)
                    view?.setSignUpButtonEnabled(true)
                }
                return@launch
            }

            // Cloud Check
            try {
                val userDoc = firestore.collection("users").document(usernameId).get().await()
                if (userDoc.exists()) {
                    withContext(Dispatchers.Main) {
                        view?.hideLoading()
                        view?.showToast("Username '$trimmedUsername' is already taken.", isError = true)
                        view?.setSignUpButtonEnabled(true)
                    }
                    return@launch
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view?.hideLoading()
                    view?.showToast("Network check failed: ${e.message}", isError = true)
                    view?.setSignUpButtonEnabled(true)
                }
                return@launch
            }

            update(50, "Creating secure account...")

            val hashedPassword = HashUtils.hashPassword(trimmedPassword)
            val newUser = User(
                username = trimmedUsername,
                password = hashedPassword,
                email = trimmedEmail,
                lastUpdated = System.currentTimeMillis()
            )

            try {
                val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                val firebaseUid = authResult.user?.uid ?: throw Exception("Auth failed")

                authResult.user?.sendEmailVerification()

                update(80, "Saving profile data...")

                userDao.registerUser(newUser)

                val publicProfileMap = hashMapOf(
                    "id" to newUser.id,
                    "firebaseUid" to firebaseUid,
                    "username" to trimmedUsername,
                    "email" to newUser.email,
                    "lastUpdated" to newUser.lastUpdated
                )

                firestore.collection("users").document(usernameId).set(publicProfileMap).await()

                update(100, "Success! Please check email to verify.")
                delay(2000)

                withContext(Dispatchers.Main) {
                    view?.hideLoading()
                    view?.navigateToLogin()
                }

            } catch (e: Exception) {
                if (auth.currentUser != null) {
                    try { auth.currentUser?.delete() } catch (delEx: Exception) { }
                }

                withContext(Dispatchers.Main) {
                    view?.hideLoading()
                    view?.setSignUpButtonEnabled(true)

                    if (e is FirebaseAuthUserCollisionException) {
                        view?.showToast("Email already exists. Please log in.", isError = true)
                    } else {
                        view?.showToast("Registration Failed: ${e.message}", isError = true)
                    }
                }
            }
        }
    }
}