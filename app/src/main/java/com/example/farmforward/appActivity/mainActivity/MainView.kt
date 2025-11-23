package com.example.farmforward.appActivity.mainActivity

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope

interface MainView {
    fun getAppActivity(): AppCompatActivity
    fun getFragManager(): FragmentManager
    fun getScope(): LifecycleCoroutineScope
    fun navigateToLogin()
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)
    fun highlightNavigation(menuId: Int)
    fun showSignOutDialog()
    fun openDrawer()
    fun closeDrawer()
}