package com.example.farmforward.utils.loadingUtils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class LoadingActivity : AppCompatActivity() {

    @Inject lateinit var syncManager: FirebaseSyncManager
    @Inject lateinit var weatherRepository: WeatherRepository
    private lateinit var seekBar: SeekBar
    private lateinit var statusText: TextView
    private var locationContinuation: CancellableContinuation<Boolean>? = null

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            locationContinuation?.resume(true)
        } else {
            locationContinuation?.resume(false)
        }
        locationContinuation = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        seekBar = findViewById(R.id.loadingSeekBar)
        statusText = findViewById(R.id.tvStatus)

        startDataSync()
    }

    private fun startDataSync() {
        lifecycleScope.launch(Dispatchers.IO) {
            updateProgress(10, "Checking connection...")
            delay(300)

            if (isNetworkAvailable()) {
                try {
                    updateProgress(30, "Syncing User Profile...")
                    syncManager.syncUsers()

                    updateProgress(50, "Downloading Crop Data...")
                    syncManager.syncCrops()
                } catch (e: Exception) {
                    Log.e("LoadingActivity", "Sync failed (Error): ${e.message}")
                    updateProgress(50, "Sync Error: Using Local Data...")
                    delay(500)
                }
            } else {
                Log.d("LoadingActivity", "No Internet: Skipping Sync")
                updateProgress(50, "Offline Mode: Using Local Data...")
                delay(800)
            }

            updateProgress(70, "Checking Weather...")
            val hasPermission = ContextCompat.checkSelfPermission(
                this@LoadingActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    val isGpsOn = ensureLocationOn()
                    if (isGpsOn) {
                        if (isNetworkAvailable()) {
                            updateProgress(75, "Fetching Local Weather...")
                            fetchWeatherSync()
                        } else {
                            updateProgress(75, "Offline: Skipping Weather...")
                            delay(500)
                        }
                    } else {
                        updateProgress(75, "GPS Disabled: Skipping Weather...")
                        delay(500)
                    }
                } catch (e: Exception) {
                    Log.e("LoadingActivity", "Weather pre-fetch failed: ${e.message}")
                }
            } else {
                Log.d("LoadingActivity", "Skipping weather: No permission yet.")
            }

            updateProgress(90, "Finalizing...")
            delay(400)

            updateProgress(100, "Ready!")
            delay(200)

            withContext(Dispatchers.Main) {
                navigateToMain()
            }
        }
    }

    // --- HELPER: Network Check ---
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private suspend fun ensureLocationOn(): Boolean = suspendCancellableCoroutine { cont ->
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            if (cont.isActive) cont.resume(true)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    locationContinuation = cont
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    if (cont.isActive) cont.resume(false)
                }
            } else {
                if (cont.isActive) cont.resume(false)
            }
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun fetchWeatherSync() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val location = fusedLocationClient.lastLocation.await() ?: return

            var locName = "Unknown"
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                locName = if (!addresses.isNullOrEmpty()) addresses[0].locality ?: "Unknown" else "Unknown"
            } catch (e: Exception) {
                Log.e("LoadingActivity", "Geocoder failed: ${e.message}")
            }

            val apiKey = BuildConfig.WEATHER_API_KEY
            val response = RetrofitClient.instance.getForecastByCoordinates(location.latitude, location.longitude, apiKey)

            if (response.isSuccessful) {
                val forecasts = response.body()?.list
                if (forecasts != null) {
                    val upcomingForecasts = forecasts.take(8)
                    val format = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                    val dateText = format.format(Date())

                    weatherRepository.saveWeatherData(upcomingForecasts, locName, dateText)
                }
            }
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Weather Fetch Failed (Offline): ${e.message}")
        }
    }

    private suspend fun updateProgress(value: Int, message: String) {
        withContext(Dispatchers.Main) {
            if (!isFinishing) {
                seekBar.progress = value
                statusText.text = message
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        val targetId = getIntent().getIntExtra("DESTINATION_ID", -1)
        if (targetId != -1) {
            intent.putExtra("DESTINATION_ID", targetId)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}