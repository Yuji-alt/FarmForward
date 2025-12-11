package com.example.farmforward.appActivity.userActivity.signUp

interface SignUpView {
    fun showToast(message: String, isError: Boolean = false)
    fun setSignUpButtonEnabled(isEnabled: Boolean)
    fun navigateToLogin()
    fun showLoading()
    fun updateLoading(progress: Int, message: String)
    fun hideLoading()
}