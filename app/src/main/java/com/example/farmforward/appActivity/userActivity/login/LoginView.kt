package com.example.farmforward.appActivity.userActivity.login

interface LoginView {
    fun showToast(message: String, isError: Boolean = false)
    fun navigateToMain()
    fun startLoginSync()
    fun navigateToSignUp()
    fun setOfflineSwitch(isChecked: Boolean)
    fun enableSignUpButton(isEnabled: Boolean)
    fun showForgotPasswordDialog()
    fun showUnverifiedAccountDialog(email: String, password: String)
}