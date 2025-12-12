package com.example.farmforward.utils.onBoarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.login.LoginActivity
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
    private lateinit var btnSkip: Button
    private lateinit var adapter: OnboardingAdapter
    private var isFromSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        // StatusBar Color
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        isFromSettings = intent.getBooleanExtra("FROM_SETTINGS", false)

        viewPager = findViewById(R.id.viewPager)
        val dotsIndicator = findViewById<WormDotsIndicator>(R.id.dotsIndicator)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        setupOnboardingItems(dotsIndicator)
        setupListeners()
    }

    private fun setupOnboardingItems(dotsIndicator: WormDotsIndicator) {
        val slides = listOf(
            // 1. HOME
            OnboardingItem(
                "Home Dashboard",
                "Get a quick overview of your weather forecast, alerts, and active crop status all in one place.",
                R.drawable.home_onboard
            ),
            // 2. GARDEN
            OnboardingItem(
                "My Garden",
                "Add and track your crops. Monitor planting dates, expected harvest, and growth progress.",
                R.drawable.garden_onboard
            ),
            // 3. CALCULATOR
            OnboardingItem(
                "Yield Calculator",
                "Estimate your potential harvest yield based on crop type, area, and plant density.",
                R.drawable.calc_onboard
            ),
            // 4. GROWTH
            OnboardingItem(
                "Growth Tracker",
                "Monitor the specific growth stages of your plants and receive timely care reminders.",
                R.drawable.growth_onboard
            ),
            // 5. MAP
            OnboardingItem(
                "Farm Map",
                "Pin your crops to real-world locations to get localized weather and soil insights.",
                R.drawable.map_onboard
            ),
            // 6. SETTINGS
            OnboardingItem(
                "Settings & Data",
                "Manage your account, toggle offline mode, and sync your data securely to the cloud.",
                R.drawable.settings_onboard
            )
        )

        adapter = OnboardingAdapter(slides)
        viewPager.adapter = adapter
        dotsIndicator.attachTo(viewPager)
    }

    private fun setupListeners() {
        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < adapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == adapter.itemCount - 1) {
                    btnNext.text = "Get Started"
                } else {
                    btnNext.text = "Next"
                }
            }
        })
    }

    private fun finishOnboarding() {
        // 1. Mark as seen (Crucial for the "First Time" logic)
        val prefs = getSharedPreferences("FarmForwardConfig", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()

        // 2. Navigation Logic
        if (isFromSettings) {
            // Just close the activity to go back to Settings
            finish()
        } else {
            // Go to Home (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            // Clear back stack so they can't go back to tutorial or login
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}