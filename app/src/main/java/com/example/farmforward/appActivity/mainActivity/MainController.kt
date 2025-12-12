package com.example.farmforward.appActivity.mainActivity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.notificationsUtils.DailyCheckWorker
import com.example.farmforward.utils.notificationsUtils.WeatherWorker
import com.example.farmforward.utils.otherUtils.NetworkUtils
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
    private var scope: LifecycleCoroutineScope? = null
    private var currentMenuId: Int = -1
    private var hasUserDeniedGps = false
    val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private var isSyncInProgress: Boolean = false

    fun bindView(view: MainView) {
        this.view = view
        this.scope = view.getScope()
    }
    fun setGpsDeniedInSession() {
        this.hasUserDeniedGps = true
    }
    fun setSyncStatus(isInProgress: Boolean) {
        this.isSyncInProgress = isInProgress
        scope?.launch(Dispatchers.Main) {
            view?.setSignOutButtonEnabled(!isInProgress)
        }
    }
    // ---------------------------------------------------------


    // --- CHECK GPS SETTINGS (Helper) ---
    fun ensureLocationSettings(activity: AppCompatActivity, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isGpsOn || isNetworkOn) {
            hasUserDeniedGps = false
            onSuccess()
            return
        }

        if (hasUserDeniedGps) {
            onFailure()
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            hasUserDeniedGps = false
            onSuccess()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    view?.launchLocationSettings(exception,
                        onSuccess = {
                            hasUserDeniedGps = false
                            onSuccess()
                        },
                        onFailure = {
                            hasUserDeniedGps = true
                            onFailure()
                        }
                    )
                } catch (sendEx: Exception) {
                    onFailure()
                }
            } else {
                onFailure()
            }
        }
    }

    // --- CHECK PERMISSIONS ---
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

    // --- FETCH LOCATION (Now checks GPS too) ---
    fun fetchCurrentLocation(activity: AppCompatActivity, onLocation: (lat: Double, lon: Double) -> Unit) {
        // 1. Check Permissions
        if (!hasLocationPermission()) {
            view?.showToast("Location permission required", isError = true)
            onLocation(0.0, 0.0)
            return
        }

        // 2. Check GPS Settings (ensureLocationSettings)
        ensureLocationSettings(activity,
            onSuccess = {
                // 3. Get Coordinates
                try {
                    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
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
            },
            onFailure = {
                onLocation(0.0, 0.0)
            }
        )
    }

    // ... (Helpers & Navigation) ...

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestSystemPermission(activity: AppCompatActivity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    fun fetchLastKnownLocation(activity: AppCompatActivity, onLocation: (lat: Double, lon: Double) -> Unit) {
        if (!hasLocationPermission()) {
            onLocation(0.0, 0.0)
            return
        }
        try {
            val client = LocationServices.getFusedLocationProviderClient(activity)
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    onLocation(location.latitude, location.longitude)
                } else {
                    fetchCurrentLocation(activity, onLocation)
                }
            }.addOnFailureListener {
                onLocation(0.0, 0.0)
            }
        } catch (e: SecurityException) {
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

    // --- UPDATED: Saved & Sync Clicked ---
    fun onSavedAndSyncClicked() {
        if (isSyncInProgress) {
            view?.showToast("Synchronization is already running.", isError = false)
            return
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            view?.showToast("Offline: Data saved locally.", isError = false)
            return
        }

        // Show Loading via Dialog
        val loadingDialog = com.example.farmforward.utils.loadingUtils.LoadingDialogFragment()
        loadingDialog.isCancelable = false

        val fragmentManager = view?.getFragManager()
        if (fragmentManager != null) {
            loadingDialog.show(fragmentManager, "SyncLoading")
        }
        setSyncStatus(true)

        scope?.launch(Dispatchers.IO) {
            fun updateProgress(progress: Int, message: String) {
                launch(Dispatchers.Main) {
                    if (loadingDialog.isAdded) loadingDialog.updateProgress(progress, message)
                }
            }

            try {
                updateProgress(20, "Syncing Profile...")
                syncManager.syncUsers()
                updateProgress(60, "Syncing Crops...")
                syncManager.syncCrops()
                updateProgress(100, "Done!")
                kotlinx.coroutines.delay(500)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    view?.showToast("Cloud Sync Complete!", isError = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    view?.showToast("Sync warning: ${e.message}", isError = true)
                }
            } finally {
                setSyncStatus(false)
            }
        }
    }

    fun onSignOutClicked() {
        view?.closeDrawer()
        scope?.launch(Dispatchers.IO) {
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
        scope?.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("FarmForwardConfig", Context.MODE_PRIVATE)
            val keepData = prefs.getBoolean("keep_data_offline", true)
            if (keepData) {
                withContext(Dispatchers.Main) {
                    view?.showToast("Offline Data Kept on Device", isError = false)
                }
            } else {
                db.clearAllTables()
            }
            session.clearSession()
            withContext(Dispatchers.Main) {
                view?.navigateToLogin()
            }
        }
    }

    fun onDestroy() {
        view = null
    }
}