package com.example.farmforward.appActivity.mainActivity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.calc.CalcFragment
import com.example.farmforward.appActivity.mainActivity.garden.GardenFragment
import com.example.farmforward.appActivity.mainActivity.growth.GrowthCropDetailsFragment
import com.example.farmforward.appActivity.mainActivity.growth.GrowthFragment
import com.example.farmforward.appActivity.mainActivity.home.HomeFragment
import com.example.farmforward.appActivity.mainActivity.map.MapFragment
import com.example.farmforward.appActivity.mainActivity.otherFragment.CropDetails.CropDetailsFragment
import com.example.farmforward.appActivity.mainActivity.otherFragment.GardenTools.GardenToolsFragment
import com.example.farmforward.appActivity.mainActivity.otherFragment.ProfileFragment
import com.example.farmforward.appActivity.mainActivity.otherFragment.Settings.SettingsFragment
import com.example.farmforward.appActivity.userActivity.login.LoginActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.notificationsUtils.DailyCheckWorker
import com.example.farmforward.utils.notificationsUtils.WeatherWorker
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainView {
    @Inject lateinit var controller: MainController
    @Inject lateinit var session: SessionManager
    private lateinit var cropViewModel: CropViewModel
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuItems: List<LinearLayout>
    private lateinit var home: LinearLayout
    private lateinit var garden: LinearLayout
    private lateinit var map: LinearLayout
    private lateinit var calc: LinearLayout
    private lateinit var growth: LinearLayout
    private lateinit var btnSignOut: TextView
    private lateinit var btnSaved: TextView
    private val fragmentMap = mutableMapOf<Int, Fragment>()
    private val orderedTabs = listOf(
        R.id.nav_home, R.id.nav_garden, R.id.nav_calc, R.id.nav_growth, R.id.nav_map
    )
    private var onLocationSettingsSuccess: (() -> Unit)? = null
    private var onLocationSettingsFailure: (() -> Unit)? = null
    companion object {
        const val NAV_CROP_DETAILS = 10001
        const val NAV_GROWTH_CROP_DETAILS = 10002
        const val NAV_PROFILE = 10003
        const val NAV_HARVEST = 10004
        const val NAV_WEATHER = 10005
        const val NAV_SETTINGS = 10006
        const val NAV_HELP = 10007
        const val NAV_CONTACT = 10008
        const val NAV_TERMS = 10009
        const val NAV_PRIVACY = 10010

    }

    private var currentMenuId: Int = -1
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            onLocationSettingsSuccess?.invoke()
        } else {
            onLocationSettingsFailure?.invoke()
        }
        // Cleanup
        onLocationSettingsSuccess = null
        onLocationSettingsFailure = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        controller.bindView(this)

        drawerLayout = findViewById(R.id.drawer_layout)
        home = findViewById(R.id.nav_home)
        garden = findViewById(R.id.nav_garden)
        map = findViewById(R.id.nav_map)
        calc = findViewById(R.id.nav_calc)
        growth = findViewById(R.id.nav_growth)
        btnSignOut = findViewById(R.id.signOut)
        btnSaved = findViewById(R.id.Saved)
        cropViewModel = ViewModelProvider(this)[CropViewModel::class.java]
        val btnCloseNav = findViewById<ImageButton>(R.id.btn_close_nav)
        val btnProfileContainer = findViewById<LinearLayout>(R.id.btn_profile_container)

        menuItems = listOf(home, garden, map, calc, growth)
        home.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_home) }
        garden.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_garden) }
        map.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_map) }
        calc.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_calc) }
        growth.setOnClickListener { controller.onNavigationItemClicked(R.id.nav_growth) }
        btnSaved.setOnClickListener { controller.onSavedAndSyncClicked() }
        btnSignOut.setOnClickListener { controller.onSignOutClicked() }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        btnCloseNav.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }
        btnProfileContainer.setOnClickListener {
            controller.onNavigationItemClicked(NAV_PROFILE)
            closeDrawer()
        }
        findViewById<LinearLayout>(R.id.btn_my_profile).setOnClickListener {
            controller.onNavigationItemClicked(R.id.nav_garden)
            closeDrawer()
        }
        val btnSettingsContainer = findViewById<LinearLayout>(R.id.btn_settings_container)

        btnSettingsContainer.setOnClickListener {
            controller.onNavigationItemClicked(NAV_SETTINGS)
            closeDrawer()
        }
        findViewById<LinearLayout>(R.id.btn_calculator).setOnClickListener {
            controller.onNavigationItemClicked(R.id.nav_calc)
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.btn_harvest_history).setOnClickListener {
            controller.onNavigationItemClicked(NAV_HARVEST)
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.btn_weather).setOnClickListener {
            controller.onNavigationItemClicked(NAV_WEATHER)
            closeDrawer()
        }
        findViewById<LinearLayout>(R.id.btn_help)?.setOnClickListener {
            controller.onNavigationItemClicked(NAV_HELP)
            closeDrawer()
        }

        findViewById<LinearLayout>(R.id.btn_contacts)?.setOnClickListener {
            controller.onNavigationItemClicked(NAV_CONTACT)
            closeDrawer()
        }
        setupSmartNotifications()
        controller.onViewCreated()
        handleNotificationIntent(intent)
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

    override fun showToast(message: String, isError: Boolean) {
        val context = this
        val rootView = window.decorView.findViewById<android.view.View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        val insets = ViewCompat.getRootWindowInsets(rootView)
        val navBarHeight = insets?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: 0
        val bufferMargin = 20.dpToPx(context).toInt()
        params.bottomMargin = navBarHeight + bufferMargin
        params.leftMargin = 20.dpToPx(context).toInt()
        params.rightMargin = 20.dpToPx(context).toInt()

        snackbarView.layoutParams = params
        snackbarView.backgroundTintList = null
        val borderDrawable = GradientDrawable()
        borderDrawable.shape = GradientDrawable.RECTANGLE
        borderDrawable.cornerRadius = 12f.dpToPx(context)
        val bgColor = ContextCompat.getColor(context, R.color.tan)
        val strokeColor = ContextCompat.getColor(context, R.color.kombuGreen)
        borderDrawable.setColor(bgColor)
        borderDrawable.setStroke(4, strokeColor)
        snackbarView.background = borderDrawable
        snackbar.setTextColor(strokeColor)
        snackbar.setActionTextColor(strokeColor)
        snackbar.setAction("OK") { snackbar.dismiss() }
        snackbar.show()
    }

    private fun setupSmartNotifications() {
        val workManager = androidx.work.WorkManager.getInstance(this)

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val weatherRequest = PeriodicWorkRequestBuilder<WeatherWorker>(3, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "WeatherWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            weatherRequest
        )

        val cropRequest = PeriodicWorkRequestBuilder<DailyCheckWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "DailyCheckWorker",
           ExistingPeriodicWorkPolicy.KEEP,
            cropRequest
        )
    }
    private fun handleNotificationIntent(intent: Intent?) {
        val targetTab = intent?.getIntExtra("DESTINATION_TAB", -1) ?: -1
        if (targetTab != -1) {
            lifecycleScope.launchWhenResumed {
                controller.onNavigationItemClicked(targetTab)
            }
        }
    }
    override fun launchLocationSettings(
        exception: ResolvableApiException,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        this.onLocationSettingsSuccess = onSuccess
        this.onLocationSettingsFailure = onFailure
        try {
            val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
            locationSettingsLauncher.launch(intentSenderRequest)
        } catch (e: Exception) {
            onFailure()
        }
    }

    private fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    override fun highlightNavigation(menuId: Int) {
        val selected = when (menuId) {
            R.id.nav_home -> home
            R.id.nav_garden -> garden
            R.id.nav_calc -> calc
            R.id.nav_growth -> growth
            R.id.nav_map -> map
            else -> null
        }

        for (item in menuItems) {
            val icon = item.getChildAt(0) as ImageView

            if (item == selected) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_unselected))
                icon.setBackgroundResource(R.drawable.nav_selected_bg)
            } else {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_selected))
                icon.setBackgroundResource(R.drawable.nav_unselected_bg)
            }
        }
    }
    override fun showSignOutDialog() {
        val prefs = getSharedPreferences("FarmForwardConfig", Context.MODE_PRIVATE)
        val keepData = prefs.getBoolean("keep_data_offline", true)

        val title: String
        val message: String
        val positiveButtonText: String

        if (keepData) {
            title = "Log Out"
            message = "You are about to log out. Your local data will be KEPT on this device for offline use."
            positiveButtonText = "Log Out"
        } else {
            title = "Log Out and Clear Data"
            message = "Warning: 'Keep Data Offline' is OFF. This will PERMANENTLY DELETE all locally saved crops."
            positiveButtonText = "Clear All"
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ ->
                controller.onSignOutConfirmed()
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }

    override fun openDrawer() = drawerLayout.openDrawer(GravityCompat.END)

    override fun closeDrawer() = drawerLayout.closeDrawer(GravityCompat.END)

    fun navigateToGrowthResult() {
        controller.onNavigationItemClicked(R.id.nav_growth)
    }

    private val detailFragments = setOf(
        NAV_CROP_DETAILS, NAV_GROWTH_CROP_DETAILS, NAV_PROFILE,
        NAV_HARVEST, NAV_WEATHER, NAV_SETTINGS, NAV_TERMS,
        NAV_PRIVACY, NAV_HELP, NAV_CONTACT
    )

    override fun switchFragment(newMenuId: Int) {
        // 1. Optimization: Don't do anything if clicking the same tab
        if (currentMenuId == newMenuId) return

        val transaction = supportFragmentManager.beginTransaction()

        // 2. Animation Logic (Refactored for clarity)
        val isOpeningDetail = newMenuId in detailFragments
        val isClosingDetail = currentMenuId in detailFragments

        when {
            isOpeningDetail || isClosingDetail -> {
                // Use pop animations for opening/closing details
                transaction.setCustomAnimations(R.anim.pop_enter, R.anim.pop_exit)
            }
            else -> {
                // Use slide animations for main tabs based on order
                val oldIndex = orderedTabs.indexOf(currentMenuId)
                val newIndex = orderedTabs.indexOf(newMenuId)
                if (newIndex > oldIndex) {
                    transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                } else {
                    transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right)
                }
            }
        }

        // 3. Hide Current Fragment
        fragmentMap[currentMenuId]?.let {
            if (it.isAdded) transaction.hide(it)
        }

        // 4. Force Refresh Logic (Fragments that should NOT be cached)
        // If the new fragment is one of these, remove the old instance first
        if (newMenuId == R.id.nav_calc ||
            newMenuId == NAV_CROP_DETAILS ||
            newMenuId == NAV_GROWTH_CROP_DETAILS) {

            fragmentMap[newMenuId]?.let {
                transaction.remove(it)
                fragmentMap.remove(newMenuId)
            }
        }

        // 5. Show or Add New Fragment
        var newFragment = fragmentMap[newMenuId]

        if (newFragment == null) {
            newFragment = getFragmentInstance(newMenuId)
            fragmentMap[newMenuId] = newFragment
            transaction.add(R.id.fragment_container, newFragment, newMenuId.toString())
        } else {
            transaction.show(newFragment)
        }

        transaction.commit()
        currentMenuId = newMenuId
        highlightNavigation(newMenuId)
    }

    private fun getFragmentInstance(menuId: Int): Fragment {
        return when (menuId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_garden -> GardenFragment()
            R.id.nav_map -> MapFragment()
            R.id.nav_calc -> CalcFragment()
            R.id.nav_growth -> GrowthFragment()
            NAV_CROP_DETAILS -> CropDetailsFragment()
            NAV_GROWTH_CROP_DETAILS -> GrowthCropDetailsFragment()
            NAV_PROFILE -> ProfileFragment()
            NAV_HARVEST -> GardenToolsFragment.newInstance("HARVEST")
            NAV_WEATHER -> GardenToolsFragment.newInstance("WEATHER")
            NAV_SETTINGS -> SettingsFragment()
            NAV_HELP -> com.example.farmforward.appActivity.mainActivity.otherFragment.TextContentFragment.newInstance(
                getString(R.string.title_help),
                getString(R.string.content_help),
                R.id.nav_home
            )
            NAV_CONTACT -> com.example.farmforward.appActivity.mainActivity.otherFragment.TextContentFragment.newInstance(
                getString(R.string.title_contact),
                getString(R.string.content_contact),
                R.id.nav_home // <--- Back to Home
            )
            NAV_TERMS -> com.example.farmforward.appActivity.mainActivity.otherFragment.TextContentFragment.newInstance(
                getString(R.string.title_terms),
                getString(R.string.content_terms),
                NAV_SETTINGS
            )
            NAV_PRIVACY -> com.example.farmforward.appActivity.mainActivity.otherFragment.TextContentFragment.newInstance(
                getString(R.string.title_privacy),
                getString(R.string.content_privacy),
                NAV_SETTINGS
            )
            else -> HomeFragment()
        }
    }

    override fun showUnsyncedDataWarning(count: Int) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Unsynced Data Found")
            .setMessage("You have $count items that haven't been uploaded. Logging out will delete them.\n\nContinue?")
            .setPositiveButton("Delete & Logout") { _, _ ->
                controller.onSignOutConfirmed()
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == controller.LOCATION_PERMISSION_REQUEST_CODE) {
            val homeFragment = supportFragmentManager.findFragmentByTag(R.id.nav_home.toString()) as? HomeFragment
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                homeFragment?.onPermissionGranted()
            } else {
                controller.handlePermissionResult(
                    this,
                    onPermanentlyDenied = {
                        val builder = AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("Weather features are disabled because location access is permanently denied. Please enable it in Settings.")
                            .setPositiveButton("Settings") { _, _ -> controller.openAppSettings(this) }
                            .setNegativeButton("Cancel") { _, _ -> homeFragment?.onPermissionDenied() }
                        val dialog = builder.create()

                        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
                        dialog.show()
                    },
                    onDenied = {
                        homeFragment?.onPermissionDenied()
                        showToast("Location permission is needed for weather updates.", isError = true)
                    }
                )
            }
        }
        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Notifications enabled!", isError = false)
            } else {
                showToast("Weather alerts will not be shown.", isError = true)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
}