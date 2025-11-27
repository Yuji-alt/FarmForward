package com.example.farmforward.appActivity.mainActivity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.calc.CalcFragment
import com.example.farmforward.appActivity.mainActivity.garden.GardenFragment
import com.example.farmforward.appActivity.mainActivity.growth.GrowthFragment
import com.example.farmforward.appActivity.mainActivity.home.HomeFragment
import com.example.farmforward.appActivity.mainActivity.map.MapFragment
import com.example.farmforward.appActivity.userActivity.login.LoginActivity
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainView {
    @Inject lateinit var controller: MainController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuItems: List<LinearLayout>

    // Nav Items
    private lateinit var home: LinearLayout
    private lateinit var garden: LinearLayout
    private lateinit var map: LinearLayout
    private lateinit var calc: LinearLayout
    private lateinit var growth: LinearLayout

    // Drawer Items
    private lateinit var btnSignOut: TextView
    private lateinit var btnSaved: TextView
    private val fragmentMap = mutableMapOf<Int, Fragment>()
    private val orderedTabs = listOf(
        R.id.nav_home, R.id.nav_garden, R.id.nav_calc, R.id.nav_growth, R.id.nav_map
    )
    private var currentMenuId: Int = R.id.nav_home

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        controller.bindView(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        home = findViewById(R.id.nav_home)
        garden = findViewById(R.id.nav_garden)
        map = findViewById(R.id.nav_map)
        calc = findViewById(R.id.nav_calc)
        growth = findViewById(R.id.nav_growth)
        btnSignOut = findViewById(R.id.signOut)
        btnSaved = findViewById(R.id.Saved)

        menuItems = listOf(home, garden, map, calc, growth)
        home.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_home) }
        garden.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_garden) }
        map.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_map) }
        calc.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_calc) }
        growth.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_growth) }
        btnSaved.setOnClickListener { controller.onSavedClicked() }
        btnSignOut.setOnClickListener { controller.onSignOutClicked() }

        controller.onViewCreated()
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    override fun getAppActivity(): AppCompatActivity = this

    override fun getFragManager(): FragmentManager = supportFragmentManager

    override fun getScope(): LifecycleCoroutineScope = lifecycleScope

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun showToast(message: String, isError: Boolean) {
        val context = this
        val rootView = findViewById<android.view.View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.topMargin = 60.dpToPx(context).toInt()
        params.leftMargin = 20.dpToPx(context).toInt()
        params.rightMargin = 20.dpToPx(context).toInt()
        snackbarView.layoutParams = params
        snackbarView.backgroundTintList = null
        val borderDrawable = GradientDrawable()
        borderDrawable.shape = GradientDrawable.RECTANGLE
        borderDrawable.cornerRadius = 12f.dpToPx(context)
        val bgColor = ContextCompat.getColor(context, R.color.tan)
        val strokeColor = ContextCompat.getColor(context, R.color.kombuGreen)
        borderDrawable.setColor(bgColor)
        borderDrawable.setStroke(4, strokeColor)
        snackbarView.background = borderDrawable
        snackbar.setTextColor(strokeColor)
        snackbar.setActionTextColor(strokeColor)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }
    private fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    override fun highlightNavigation(menuId: Int) {
        val selected = when (menuId) {
            R.id.nav_home -> home
            R.id.nav_garden -> garden
            R.id.nav_calc -> calc
            R.id.nav_growth -> growth
            R.id.nav_map -> map
            else -> home
        }

        for (item in menuItems) {
            val icon = item.getChildAt(0) as ImageView
            val label = item.getChildAt(1) as TextView

            if (item == selected) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_selected))
                label.setTextColor(ContextCompat.getColor(this, R.color.nav_selected))
                icon.setBackgroundResource(R.drawable.nav_selected_bg)
                item.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
            } else {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_unselected))
                label.setTextColor(ContextCompat.getColor(this, R.color.nav_unselected))
                icon.setBackgroundResource(R.drawable.nav_unselected_bg)
                item.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }
    }

    override fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log Out and Clear Data")
            .setMessage("Are you sure? This will delete all locally saved crops and data.")
            .setPositiveButton("Clear All") { _, _ ->
                controller.onSignOutConfirmed()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun openDrawer() = drawerLayout.openDrawer(GravityCompat.END)

    override fun closeDrawer() = drawerLayout.closeDrawer(GravityCompat.END)

    fun navigateToGrowthResult() {
        controller.onNavigationItemClicked(R.id.nav_growth)
    }
    override fun switchFragment(newMenuId: Int) {
        val transaction = supportFragmentManager.beginTransaction()

        val oldIndex = orderedTabs.indexOf(currentMenuId)
        val newIndex = orderedTabs.indexOf(newMenuId)
        val isMovingForward = newIndex > oldIndex

        if (isMovingForward) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val currentFragment = fragmentMap[currentMenuId]
        if (currentFragment != null && currentFragment.isAdded) {
            transaction.hide(currentFragment)
        }

        var newFragment = fragmentMap[newMenuId]

        if (newMenuId == R.id.nav_calc || newMenuId == R.id.nav_growth) {
            if (newFragment != null) {
                transaction.remove(newFragment)
                fragmentMap.remove(newMenuId)
            }
            newFragment = null
        }

        if (newFragment == null) {
            newFragment = getFragmentInstance(newMenuId)
            fragmentMap[newMenuId] = newFragment
            transaction.add(R.id.fragment_container, newFragment, newMenuId.toString())
        } else {
            transaction.show(newFragment)
        }

        transaction.commit()
        currentMenuId = newMenuId
        highlightNavigation(newMenuId)
    }
    private fun getFragmentInstance(menuId: Int): Fragment {
        return when (menuId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_garden -> GardenFragment()
            R.id.nav_map -> MapFragment()
            R.id.nav_calc -> CalcFragment()
            R.id.nav_growth -> GrowthFragment()
            else -> HomeFragment()
        }
    }
    override fun showUnsyncedDataWarning(count: Int) {
        AlertDialog.Builder(this)
            .setTitle("Unsynced Data Found")
            .setMessage("You have $count items that haven't been uploaded. Logging out will delete them.\n\nContinue?")
            .setPositiveButton("Delete & Logout") { _, _ ->
                controller.onSignOutConfirmed()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == controller.LOCATION_PERMISSION_REQUEST_CODE) {
            val homeFragment = supportFragmentManager.findFragmentByTag(R.id.nav_home.toString()) as? HomeFragment
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                homeFragment?.onPermissionGranted()
            } else {
                controller.handlePermissionResult(
                    this,
                    onPermanentlyDenied = {
                        AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Weather features are disabled because location access is permanently denied. Please enable it in Settings.")
                            .setPositiveButton("Settings") { _, _ -> controller.openAppSettings(this) }
                            .setNegativeButton("Cancel") { _, _ -> homeFragment?.onPermissionDenied() }
                            .show()
                    },
                    onDenied = {
                        // Just show the standard error text
                        homeFragment?.onPermissionDenied()
                        showToast("Location permission is needed for weather updates.", isError = true)
                    }
                )
            }
        }
    }
}