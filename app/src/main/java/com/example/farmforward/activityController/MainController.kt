package com.example.farmforward.activityController

import GardenFragment
import android.content.Context
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.farmforward.R
import com.example.farmforward.fragment.HomeFragment
import com.example.farmforward.fragment.MapFragment

class MainController(
    private val context: Context,
    private val fragmentManager: FragmentManager
) {
    private val fragmentMap = mutableMapOf<Int, Fragment>()

    // Only 3 available fragments
    fun onMenuItemSelected(menuId: Int): Fragment {
        return when (menuId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_garden -> GardenFragment()
            R.id.nav_map -> MapFragment()
            else -> HomeFragment()
        }
    }

    fun switchFragment(menuId: Int) {
        val transaction = fragmentManager.beginTransaction()

        fragmentManager.fragments.forEach { transaction.hide(it) }

        val fragment = fragmentMap.getOrPut(menuId) { onMenuItemSelected(menuId) }

        if (!fragment.isAdded) {
            transaction.add(R.id.fragment_container, fragment)
        } else {
            transaction.show(fragment)
        }

        transaction.commit()

        setActiveMenu(menuId)
        (context as? AppCompatActivity)?.window?.decorView?.post {
            if (fragment is HomeFragment && fragment.isAdded && fragment.isVisible) {
                fragment.refreshData()
            }
        }
    }


    fun highlightSelected(selected: LinearLayout, allItems: List<LinearLayout>) {
        for (item in allItems) {
            val icon = item.getChildAt(0) as ImageView
            val label = item.getChildAt(1) as TextView

            if (item == selected) {
                icon.setColorFilter(ContextCompat.getColor(context, R.color.nav_selected))
                label.setTextColor(ContextCompat.getColor(context, R.color.nav_selected))
                icon.setBackgroundResource(R.drawable.nav_selected_bg)
                item.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
            } else {
                icon.setColorFilter(ContextCompat.getColor(context, R.color.nav_unselected))
                label.setTextColor(ContextCompat.getColor(context, R.color.nav_unselected))
                icon.setBackgroundResource(R.drawable.nav_unselected_bg)
                item.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }
        }
    }
    fun setActiveMenu(menuId: Int) {
        val activity = context as? AppCompatActivity ?: return
        val home = activity.findViewById<LinearLayout>(R.id.nav_home)
        val garden = activity.findViewById<LinearLayout>(R.id.nav_garden)
        val map = activity.findViewById<LinearLayout>(R.id.nav_map)
        val calc = activity.findViewById<LinearLayout>(R.id.nav_calc)
        val growth = activity.findViewById<LinearLayout>(R.id.nav_growth)
        val menuItems = listOf(home, garden, calc, growth, map)

        val selected = when (menuId) {
            R.id.nav_home -> home
            R.id.nav_garden -> garden
            R.id.nav_calc -> calc
            R.id.nav_growth -> growth
            R.id.nav_map -> map
            else -> home
        }

        highlightSelected(selected, menuItems)
    }
    fun getFragment(menuId: Int): Fragment? {
        return fragmentMap[menuId]
    }

}
