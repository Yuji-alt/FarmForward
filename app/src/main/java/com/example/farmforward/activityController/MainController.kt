package com.example.farmforward.activityController

import GardenFragment
import HomeFragment
import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.farmforward.R
import com.example.farmforward.fragment.CalcFragment
import com.example.farmforward.fragment.GrowthFragment
import com.example.farmforward.fragment.MapFragment

class MainController(
    private val context: Context,
    private val fragmentManager: FragmentManager
) {

    // Handles fragment replacement
    fun replaceFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Handles bottom menu highlight logic
    fun highlightSelected(selected: LinearLayout, allItems: List<LinearLayout>) {
        for (item in allItems) {
            val icon = item.getChildAt(0) as ImageView
            val label = item.getChildAt(1) as TextView

            if (item == selected) {
                icon.setColorFilter(ContextCompat.getColor(context, R.color.nav_selected))
                label.setTextColor(ContextCompat.getColor(context, R.color.nav_selected))
            } else {
                icon.setColorFilter(ContextCompat.getColor(context, R.color.nav_unselected))
                label.setTextColor(ContextCompat.getColor(context, R.color.nav_unselected))
            }
        }
    }

    // Chooses which fragment to load when a menu is clicked
    fun onMenuItemSelected(menuId: Int): Fragment {
        return when (menuId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_garden -> GardenFragment()
            R.id.nav_calc -> CalcFragment()
            R.id.nav_growth -> GrowthFragment()
            R.id.nav_map -> MapFragment()
            else -> HomeFragment()
        }
    }
}
