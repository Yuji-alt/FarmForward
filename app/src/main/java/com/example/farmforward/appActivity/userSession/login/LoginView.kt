package com.example.farmforward.appActivity.userSession.login

import android.content.Context
import android.widget.Toast

interface LoginView {
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)
    fun navigateToMain()
    fun startLoginSync()
    fun navigateToSignUp()
    fun setOfflineSwitch(isChecked: Boolean)
    fun enableSignUpButton(isEnabled: Boolean)
}