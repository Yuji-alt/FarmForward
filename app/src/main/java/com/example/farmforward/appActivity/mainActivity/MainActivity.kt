package com.example.farmforward.appActivity.mainActivity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.userSession.login.LoginActivity
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        controller.onPermissionResult(requestCode, permissions, grantResults)
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

    override fun showToast(message: String, duration: Int) {
        Toast.makeText(this, message, duration).show()
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
}