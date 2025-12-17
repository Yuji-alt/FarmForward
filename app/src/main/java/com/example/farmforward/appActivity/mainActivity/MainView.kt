package com.example.farmforward.appActivity.mainActivity

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.gms.common.api.ResolvableApiException
//Its a contract on Activity file on what function it needs in the MAIN VIEW
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
    fun launchLocationSettings(exception: ResolvableApiException, onSuccess: () -> Unit, onFailure: () -> Unit)
    fun setSignOutButtonEnabled(isEnabled: Boolean)
}