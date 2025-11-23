package com.example.farmforward.appActivity.userSession.signUp

import android.content.Context
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.example.farmforward.utils.NetworkUtils
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
    private val firebaseRepo: FirebaseUserRepository
) {

    private var view: SignUpView? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun bindView(view: SignUpView) {
        this.view = view
    }

    fun onSignUpClicked(username: String, password: String, confirm: String) {
        val trimmedUsername = username.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirm = confirm.trim()

        if (trimmedUsername.isEmpty() || trimmedPassword.isEmpty() || trimmedConfirm.isEmpty()) {
            view?.showToast("Please fill in all fields")
            return
        }

        if (trimmedPassword.length < 6) {
            view?.showToast("Password must be at least 6 characters")
            return
        }

        if (trimmedPassword != trimmedConfirm) {
            view?.showToast("Passwords do not match")
            return
        }
        view?.setSignUpButtonEnabled(false)

        ioScope.launch {
            // Use injected DAO
            val exists = userDao.checkUserExists(trimmedUsername)
            if (exists > 0) {
                withContext(Dispatchers.Main) {
                    view?.showToast("Username already exists!")
                    view?.setSignUpButtonEnabled(true)
                }
            } else {
                val user = User(
                    username = trimmedUsername,
                    password = trimmedPassword,
                    lastUpdated = System.currentTimeMillis()
                )

                // Save to Local DB
                userDao.registerUser(user)

                // Save to Firebase
                if (NetworkUtils.isNetworkAvailable(context)) {
                    firebaseRepo.registerUser(user)
                }

                withContext(Dispatchers.Main) {
                    view?.showToast("Registration successful!")
                    view?.navigateToLogin()
                }
            }
        }
    }

    fun onDestroy() {
        view = null
        ioScope.cancel()
    }
}