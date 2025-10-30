package com.example.farmforward.activityViewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.activityController.MainController
import com.example.farmforward.firebase.FirebaseSyncManager
import com.example.farmforward.fragment.HomeFragment
import com.example.farmforward.session.SessionManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var controller: MainController
    private lateinit var menuItems: List<LinearLayout>
    private lateinit var home: LinearLayout
    private lateinit var garden: LinearLayout
    private lateinit var map: LinearLayout
    var shouldRefreshHome = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        controller = MainController(this, supportFragmentManager)

        menuItems = listOf(home, garden, map)

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

        garden.setOnClickListener {
            controller.switchFragment(R.id.nav_garden)
            controller.setActiveMenu(R.id.nav_garden)
        }

        map.setOnClickListener {
            controller.switchFragment(R.id.nav_map)
            controller.setActiveMenu(R.id.nav_map)
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                FirebaseSyncManager(this@MainActivity).pushLocalToFirebase()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sync = FirebaseSyncManager(this@MainActivity)
                sync.pushLocalToFirebase()
                sync.syncUsers()
                sync.syncCrops()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
