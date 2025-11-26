package com.example.farmforward.appActivity.userActivity.login

import android.widget.Toast

interface LoginView {
    fun showToast(message: String, isError: Boolean = false)
    fun navigateToMain()
    fun startLoginSync()
    fun navigateToSignUp()
    fun setOfflineSwitch(isChecked: Boolean)
    fun enableSignUpButton(isEnabled: Boolean)
    fun showForgotPasswordDialog()
}