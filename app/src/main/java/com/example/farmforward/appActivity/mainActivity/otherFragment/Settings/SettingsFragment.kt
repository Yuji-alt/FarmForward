package com.example.farmforward.appActivity.mainActivity.otherFragment.Settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_profile)
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_home)
        }
        setupClickListeners(view)

        return view
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<LinearLayout>(R.id.btn_clear_data).setOnClickListener {
            (activity as? MainActivity)?.showToast("Feature coming soon", false)
        }
    }
}