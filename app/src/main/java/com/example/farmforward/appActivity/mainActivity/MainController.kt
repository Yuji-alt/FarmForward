package com.example.farmforward.appActivity.mainActivity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.farmforward.R
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager // Import this
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.viewModel.CropViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionManager,
    private val db: AppDatabase,
    private val syncManager: FirebaseSyncManager
) {
    private var view: MainView? = null

    private var currentMenuId: Int = R.id.nav_home
    val LOCATION_PERMISSION_REQUEST_CODE = 1001

    fun bindView(view: MainView) {
        this.view = view
    }
    fun ensureLocationSettings(activity: AppCompatActivity, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            onSuccess()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    (view as? MainActivity)?.launchLocationSettings(exception, onSuccess, onFailure)
                } catch (sendEx: Exception) {
                    onFailure()
                }
            } else {
                onFailure()
            }
        }
    }

    fun onViewCreated() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        if (!session.isLoggedIn()) {
            view?.navigateToLogin()
            return
        }
        view?.switchFragment(R.id.nav_home)
    }

    fun onNavigationItemClicked(menuId: Int) {
        if (menuId == currentMenuId) return

        currentMenuId = menuId
        view?.switchFragment(menuId)
    }

    fun onBackClicked(viewModel: CropViewModel) {
        val targetId = if (viewModel.lastSourceId != 0) viewModel.lastSourceId else R.id.nav_home
        onNavigationItemClicked(targetId)
    }

    fun onSavedAndSyncClicked() {
        view?.closeDrawer()
        view?.showToast("Syncing data to cloud...", isError = false)
        view?.getScope()?.launch(Dispatchers.IO) {
            syncManager.syncUsers()
            syncManager.syncCrops()
            session.clearSession()
            withContext(Dispatchers.Main) {
                view?.showToast("Data saved & Logged out!", isError = false)
                view?.navigateToLogin()
            }
        }
    }

    fun onSignOutClicked() {
        view?.closeDrawer()
        view?.getScope()?.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            val unsyncedList = db.cropDao().getUnsyncedCrops(userId)
            val count = unsyncedList.size

            withContext(Dispatchers.Main) {
                if (count > 0) {
                    view?.showUnsyncedDataWarning(count)
                } else {
                    view?.showSignOutDialog()
                }
            }
        }
    }
    fun onSignOutConfirmed() {
        view?.closeDrawer()
        view?.showToast("Syncing data before logout...", isError = false)

        view?.getScope()?.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1

            // 1. ADDED: Try to Sync first (Safety Backup)
            try {
                syncManager.syncUsers()
                syncManager.syncCrops()
            } catch (e: Exception) {
                // If sync fails (e.g., no internet), we just continue to logout.
                // We don't want to trap the user if they really want to sign out.
            }

            // 2. Delete local data (Clean Slate for next user)
            db.cropDao().deleteAllCropsForUser(userId)

            // 3. Clear Session and Navigate
            session.clearSession()
            withContext(Dispatchers.Main) {
                view?.navigateToLogin()
            }
        }
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
    fun fetchCurrentLocation(activity: AppCompatActivity, onLocation: (lat: Double, lon: Double) -> Unit) {
        if (!hasLocationPermission()) {
            view?.showToast("Location permission required", isError = true)
            onLocation(0.0, 0.0)
            return
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        onLocation(location.latitude, location.longitude)
                    } else {
                        view?.showToast("Unable to get location", isError = true)
                        onLocation(0.0, 0.0)
                    }
                }
                .addOnFailureListener {
                    view?.showToast("Error getting location", isError = true)
                    onLocation(0.0, 0.0)
                }
        } catch (e: SecurityException) {
            view?.showToast("Location permission denied", isError = true)
            onLocation(0.0, 0.0)
        }
    }
    fun handlePermissionResult(
        activity: AppCompatActivity,
        onPermanentlyDenied: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (hasLocationPermission()) return
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            onPermanentlyDenied()
        } else {
            onDenied()
        }
    }
    fun openAppSettings(activity: AppCompatActivity) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", activity.packageName, null)
        )
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }

    fun onDestroy() {
        view = null
    }
}