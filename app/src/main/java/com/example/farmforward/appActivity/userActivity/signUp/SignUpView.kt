package com.example.farmforward.appActivity.userActivity.signUp

import android.widget.Toast

interface SignUpView {
    fun showToast(message: String, isError: Boolean = false)
    fun setSignUpButtonEnabled(isEnabled: Boolean)
    fun navigateToLogin()
}