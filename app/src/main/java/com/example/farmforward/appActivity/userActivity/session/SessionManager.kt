package com.example.farmforward.appActivity.userActivity.session

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_OFFLINE_MODE = "offline_mode"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    fun saveSession(userId: Int, username: String, email: String) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_EMAIL, email)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun saveOfflineMode(isOffline: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_OFFLINE_MODE, isOffline)
            apply()
        }
    }

    fun isOfflineMode(): Boolean {
        return prefs.getBoolean(KEY_OFFLINE_MODE, false)
    }

    fun getUserId(): Int? {
        val id = prefs.getInt(KEY_USER_ID, -1)
        return if (id != -1) id else null
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USERNAME, "Farmer")
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_EMAIL, "No Email Found")
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
    fun clearSession() {
        prefs.edit().clear().apply()
    }
    fun createLoginSession(userId: Int, username: String, email: String) {
        saveSession(userId, username, email)
    }
    fun getUserDetails(): HashMap<String, String> {
        val user = HashMap<String, String>()
        // Use "name" as the key to match your SettingsFragment logic
        user["name"] = prefs.getString(KEY_USERNAME, null) ?: ""
        user["email"] = prefs.getString(KEY_EMAIL, null) ?: ""
        return user
    }
}