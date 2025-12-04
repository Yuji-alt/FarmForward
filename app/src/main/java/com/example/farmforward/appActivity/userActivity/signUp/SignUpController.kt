package com.example.farmforward.appActivity.userActivity.signUp

import android.content.Context
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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

    // -------------------------------------------------------------------------
    // Variables & Scope
    // -------------------------------------------------------------------------
    private var view: SignUpView? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    // -------------------------------------------------------------------------
    // Lifecycle & Binding
    // -------------------------------------------------------------------------
    fun bindView(view: SignUpView) {
        this.view = view
    }

    fun onDestroy() {
        view = null
        ioScope.cancel()
    }

    // -------------------------------------------------------------------------
    // Core Business Logic (Sign Up)
    // -------------------------------------------------------------------------
    fun onBackClicked() {
        view?.navigateToLogin()
    }

    fun onSignUpClicked(email: String, username: String, password: String, confirm: String) {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirm.trim()

        // 1. Basic Input Validation
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

        // 2. STRICT NETWORK CHECK (Online Only)
        if (!NetworkUtils.isNetworkAvailable(context)) {
            view?.showToast("Internet connection is required to sign up.", isError = true)
            return
        }

        // Disable button to prevent double clicks
        view?.setSignUpButtonEnabled(false)

        ioScope.launch {
            // 3. Local DB Check (Quick Fail)
            // Even though we are online, check if this phone already has this user locally
            val localExists = userDao.checkUserExists(trimmedUsername)
            if (localExists > 0) {
                withContext(Dispatchers.Main) {
                    view?.showToast("Username already exists on this device!", isError = true)
                    view?.setSignUpButtonEnabled(true)
                }
                return@launch
            }

            // 4. Cloud DB Check (Check for duplicates globally)
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("username", trimmedUsername)
                    .get()
                    .await()

                if (!snapshot.isEmpty) {
                    withContext(Dispatchers.Main) {
                        view?.showToast("Username is already taken.", isError = true)
                        view?.setSignUpButtonEnabled(true)
                    }
                    return@launch
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    view?.showToast("Network error checking username.", isError = true)
                    view?.setSignUpButtonEnabled(true)
                }
                return@launch
            }

            // 5. Proceed to Create Account
            val newUser = User(
                username = trimmedUsername,
                password = trimmedPassword,
                email = trimmedEmail,
                lastUpdated = System.currentTimeMillis()
            )

            try {
                auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword)
                    .addOnSuccessListener {
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
        }
    }

    // -------------------------------------------------------------------------
    // Database Operations
    // -------------------------------------------------------------------------
    private suspend fun saveUserToDatabases(user: User) {
        // Save Local
        userDao.registerUser(user)

        // Save Cloud (Guaranteed online at this point)
        firebaseRepo.registerUser(user)

        withContext(Dispatchers.Main) {
            view?.showToast("Registration successful!", isError = false)
            view?.navigateToLogin()
        }
    }
}