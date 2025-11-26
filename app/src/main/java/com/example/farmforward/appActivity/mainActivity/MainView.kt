package com.example.farmforward.appActivity.mainActivity

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager // You can remove this import if you remove the method below
import androidx.lifecycle.LifecycleCoroutineScope

interface MainView {
    fun getAppActivity(): AppCompatActivity

    fun getFragManager(): FragmentManager

    fun getScope(): LifecycleCoroutineScope
    fun navigateToLogin()
    fun showToast(message: String, isError: Boolean = false)
    fun highlightNavigation(menuId: Int)
    fun showSignOutDialog()
    fun openDrawer()
    fun closeDrawer()
    fun switchFragment(newMenuId: Int)
    fun showUnsyncedDataWarning(count: Int)
}