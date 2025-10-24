package com.example.farmforward.activityViewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.farmforward.R
import com.example.farmforward.activityController.MainController
import com.example.farmforward.session.SessionManager

class MainActivity : AppCompatActivity() {

    private lateinit var controller: MainController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionManager(this)
        if (!session.isLoggedIn()) {
            // No valid session → back to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)

        controller = MainController(this, supportFragmentManager)

        controller.replaceFragment(controller.onMenuItemSelected(R.id.nav_home))

        val home = findViewById<LinearLayout>(R.id.nav_home)
        val garden = findViewById<LinearLayout>(R.id.nav_garden)
        val calc = findViewById<LinearLayout>(R.id.nav_calc)
        val growth = findViewById<LinearLayout>(R.id.nav_growth)
        val map = findViewById<LinearLayout>(R.id.nav_map)
        val menuItems = listOf(home, garden, calc, growth, map)

        for (item in menuItems) {
            item.setOnClickListener {
                val selectedFragment = controller.onMenuItemSelected(item.id)
                controller.replaceFragment(selectedFragment)
                controller.highlightSelected(item, menuItems)
            }
        }
    }
}
