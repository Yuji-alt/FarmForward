package com.example.farmforward
import GardenFragment
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.farmforward.fragment.CalcFragment
import com.example.farmforward.fragment.GrowthFragment
import com.example.farmforward.fragment.HomeFragment
import com.example.farmforward.fragment.MapFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()

        val home = findViewById<LinearLayout>(R.id.nav_home)
        val garden = findViewById<LinearLayout>(R.id.nav_garden)
        val calc = findViewById<LinearLayout>(R.id.nav_calc)
        val growth = findViewById<LinearLayout>(R.id.nav_growth)
        val map = findViewById<LinearLayout>(R.id.nav_map)

        home.setOnClickListener {
            replaceFragment(HomeFragment())
            highlightSelected(home)
        }
        garden.setOnClickListener {
            replaceFragment(GardenFragment())
            highlightSelected(garden)
        }
        calc.setOnClickListener {
            replaceFragment(CalcFragment())
            highlightSelected(calc)
        }
        growth.setOnClickListener {
            replaceFragment(GrowthFragment())
            highlightSelected(growth)
        }
        map.setOnClickListener {
            replaceFragment(MapFragment())
            highlightSelected(map)
        }
    }

    private fun highlightSelected(selected: LinearLayout) {
        val menuItems = listOf(
            findViewById<LinearLayout>(R.id.nav_home),
            findViewById<LinearLayout>(R.id.nav_garden),
            findViewById<LinearLayout>(R.id.nav_calc),
            findViewById<LinearLayout>(R.id.nav_growth),
            findViewById<LinearLayout>(R.id.nav_map)
        )

        for (item in menuItems) {
            val icon = item.getChildAt(0) as ImageView
            val label = item.getChildAt(1) as TextView

            if (item == selected) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_selected))
                label.setTextColor(ContextCompat.getColor(this, R.color.nav_selected))
            } else {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_unselected))
                label.setTextColor(ContextCompat.getColor(this, R.color.nav_unselected))
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}