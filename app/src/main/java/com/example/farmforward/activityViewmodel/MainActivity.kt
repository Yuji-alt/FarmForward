package com.example.farmforward.activityViewmodel

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.activityController.MainController
import com.example.farmforward.firebase.FirebaseSyncManager
import com.example.farmforward.fragment.GardenFragment
import com.example.farmforward.fragment.HomeFragment
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.session.SessionManager
import com.example.farmforward.utils.NetworkUtils
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    lateinit var controller: MainController
    private lateinit var menuItems: List<LinearLayout>
    private lateinit var home: LinearLayout
    private lateinit var garden: LinearLayout
    private lateinit var map: LinearLayout
    private lateinit var calc: LinearLayout
    private lateinit var growth: LinearLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var btnSignOut: TextView
    var shouldRefreshHome = false
    private var hasSynced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controller = MainController(this, supportFragmentManager)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        home = findViewById(R.id.nav_home)
        garden = findViewById(R.id.nav_garden)
        map = findViewById(R.id.nav_map)
        calc = findViewById(R.id.nav_calc)
        growth = findViewById(R.id.nav_growth)
        btnSignOut = findViewById(R.id.signOut)
        drawerLayout = findViewById(R.id.drawer_layout)
        menuItems = listOf(home, garden, map, calc, growth)

        controller.switchFragment(R.id.nav_home)
        controller.highlightSelected(home, menuItems)

        home.setOnClickListener {
            controller.switchFragment(R.id.nav_home)
            controller.setActiveMenu(R.id.nav_home)
            home.postDelayed({
                if (shouldRefreshHome) {
                    val homeFragment = controller.getFragment(R.id.nav_home) as? HomeFragment
                    homeFragment?.refreshData()
                    shouldRefreshHome = false
                }
            }, 100)
        }
        home.setOnClickListener {
            controller.switchFragment(R.id.nav_home)
        }

        garden.setOnClickListener {
            controller.switchFragment(R.id.nav_garden)
            controller.setActiveMenu(R.id.nav_garden)
            val gardenFragment = controller.getFragment(R.id.nav_garden) as? GardenFragment
            gardenFragment?.refreshData()
        }

        map.setOnClickListener {
            controller.switchFragment(R.id.nav_map)
            controller.setActiveMenu(R.id.nav_map)
        }
        calc.setOnClickListener {
            controller.switchFragment(R.id.nav_calc)
            controller.setActiveMenu(R.id.nav_calc)
        }

        growth.setOnClickListener {
            controller.switchFragment(R.id.nav_growth)
            controller.setActiveMenu(R.id.nav_growth)
        }

        // One-time sync
        lifecycleScope.launch(Dispatchers.IO) {
            val isOnline = NetworkUtils.isNetworkAvailable(this@MainActivity)
            val db = AppDatabase.getDatabase(this@MainActivity)
            val userId = session.getUserId() ?: return@launch
            val localCount = db.cropDao().countUserCrops(userId)
            if (!hasSynced && isOnline && localCount == 0) {
                hasSynced = true
                try {
                    val sync = FirebaseSyncManager(this@MainActivity)
                    sync.syncUsers()
                    sync.syncCrops()

                    withContext(Dispatchers.Main) {
                        val homeFragment = controller.getFragment(R.id.nav_home) as? HomeFragment
                        homeFragment?.refreshData()
                        val gardenFragment = controller.getFragment(R.id.nav_garden) as? GardenFragment
                        gardenFragment?.refreshData()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                withContext(Dispatchers.Main) {
                    val homeFragment = controller.getFragment(R.id.nav_home) as? HomeFragment
                    homeFragment?.refreshData()
                    val gardenFragment = controller.getFragment(R.id.nav_garden) as? GardenFragment
                    gardenFragment?.refreshData()
                }
            }
        }

        // Logout handler
        btnSignOut.setOnClickListener {
            session.clearSession()
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == controller.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val homeFragment = controller.getFragment(R.id.nav_home) as? HomeFragment
                homeFragment?.onPermissionGranted()
            } else {
                controller.handlePermissionPermanentlyDenied(this) {
                    val homeFragment = controller.getFragment(R.id.nav_home) as? HomeFragment
                    homeFragment?.onPermissionDenied()
                }
            }
        }
    }

    fun openDrawer() = drawerLayout.openDrawer(GravityCompat.END)
    fun closeDrawer() = drawerLayout.closeDrawer(GravityCompat.END)
}
