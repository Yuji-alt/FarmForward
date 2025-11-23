package com.example.farmforward.appActivity.mainActivity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.farmforward.R
import com.example.farmforward.appActivity.calc.CalcFragment
import com.example.farmforward.appActivity.garden.GardenFragment
import com.example.farmforward.appActivity.growth.GrowthFragment
import com.example.farmforward.appActivity.home.HomeFragment
import com.example.farmforward.appActivity.map.MapFragment
import com.example.farmforward.appActivity.userSession.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionManager,
    private val db: AppDatabase
) {

    private var view: MainView? = null

    private lateinit var fragmentManager: FragmentManager
    private lateinit var activity: AppCompatActivity

    private val fragmentMap = mutableMapOf<Int, Fragment>()
    val LOCATION_PERMISSION_REQUEST_CODE = 1001

    fun bindView(view: MainView) {
        this.view = view
        this.activity = view.getAppActivity()
        this.fragmentManager = view.getFragManager()
    }

    fun onViewCreated() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        if (!session.isLoggedIn()) {
            view?.navigateToLogin()
            return
        }
        switchFragment(R.id.nav_home)
    }

    fun onNavigationItemClicked(menuId: Int) {
        switchFragment(menuId)
    }

    fun onSavedClicked() {
        session.clearSession()
        view?.closeDrawer()
        view?.showToast("Logged out. Offline data is saved.")
        view?.navigateToLogin()
    }

    fun onSignOutClicked() {
        view?.closeDrawer()
        view?.showSignOutDialog()
    }

    fun onSignOutConfirmed() {
        view?.getScope()?.launch(Dispatchers.IO) {
            db.clearAllTables()
            session.clearSession()
            withContext(Dispatchers.Main) {
                view?.navigateToLogin()
            }
        }
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val homeFragment = getFragment(R.id.nav_home) as? HomeFragment
                homeFragment?.onPermissionGranted()
            } else {
                handlePermissionPermanentlyDenied(activity) {
                    val homeFragment = getFragment(R.id.nav_home) as? HomeFragment
                    homeFragment?.onPermissionDenied()
                }
            }
        }
    }

    private fun onMenuItemSelected(menuId: Int): Fragment {
        return when (menuId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_garden -> GardenFragment()
            R.id.nav_map -> MapFragment()
            R.id.nav_calc -> CalcFragment()
            R.id.nav_growth -> GrowthFragment()
            else -> HomeFragment()
        }
    }

    fun switchFragment(menuId: Int) {
        if (!::fragmentManager.isInitialized) return

        val transaction = fragmentManager.beginTransaction()
        transaction.setCustomAnimations(
            R.anim.slide_in_up,
            R.anim.fade_out,
            R.anim.fade_in,
            R.anim.slide_out_down
        )
        fragmentManager.fragments.forEach { transaction.hide(it) }

        val fragment = fragmentMap.getOrPut(menuId) { onMenuItemSelected(menuId) }

        if (!fragment.isAdded) {
            transaction.add(R.id.fragment_container, fragment)
        } else {
            transaction.show(fragment)
        }

        transaction.commit()
        view?.highlightNavigation(menuId)
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkAndRequestLocationPermission(
        activity: AppCompatActivity,
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit
    ) {
        when {
            hasLocationPermission() -> {
                onPermissionGranted()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                AlertDialog.Builder(activity)
                    .setTitle(R.string.permission_needed)
                    .setMessage(R.string.location_permission_rationale)
                    .setPositiveButton("OK") { _, _ ->
                        requestSystemPermission(activity)
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        onPermissionDenied()
                    }
                    .create()
                    .show()
            }

            else -> {
                requestSystemPermission(activity)
            }
        }
    }

    fun requestSystemPermission(activity: AppCompatActivity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    fun handlePermissionPermanentlyDenied(activity: AppCompatActivity, onPermissionDenied: () -> Unit) {
        if (!hasLocationPermission() && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.permission_denied)
                .setMessage(R.string.permission_denied_permanently_message)
                .setPositiveButton("Go to Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    activity.startActivity(intent)
                }
                .setNegativeButton("Not now") { dialog, _ ->
                    dialog.dismiss()
                    onPermissionDenied()
                }
                .create()
                .show()
        } else if (!hasLocationPermission()) {
            onPermissionDenied()
        }
    }

    fun fetchCurrentLocation(activity: AppCompatActivity, onLocation: (lat: Double, lon: Double) -> Unit) {
        if (!hasLocationPermission()) {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        onLocation(location.latitude, location.longitude)
                    } else {
                        Toast.makeText(context, "Unable to get location", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun getFragment(menuId: Int): Fragment? {
        return fragmentMap[menuId]
    }

    fun onDestroy() {
        view = null
    }
}