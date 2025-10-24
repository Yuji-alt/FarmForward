package com.example.farmforward.activityViewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.activityController.MainController
import com.example.farmforward.firebase.FirebaseSyncManager
import com.example.farmforward.session.SessionManager
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var controller: MainController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        controller = MainController(this, supportFragmentManager)

        val home = findViewById<LinearLayout>(R.id.nav_home)
        val garden = findViewById<LinearLayout>(R.id.nav_garden)
        val calc = findViewById<LinearLayout>(R.id.nav_calc)
        val growth = findViewById<LinearLayout>(R.id.nav_growth)
        val map = findViewById<LinearLayout>(R.id.nav_map)
        val menuItems = listOf(home, garden, calc, growth, map)

        controller.switchFragment(R.id.nav_home)
        controller.highlightSelected(home, menuItems)

        for (item in menuItems) {
            item.setOnClickListener {
                controller.switchFragment(item.id)
                controller.highlightSelected(item, menuItems)
            }
        }

        // ✅ Safe Firebase sync
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
