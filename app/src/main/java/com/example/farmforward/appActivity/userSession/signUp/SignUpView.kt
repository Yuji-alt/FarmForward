package com.example.farmforward.appActivity.userSession.signUp

import android.content.Context
import android.widget.Toast

interface SignUpView {
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)
    fun setSignUpButtonEnabled(isEnabled: Boolean)
    fun navigateToLogin()
}