package com.example.farmforward.utils.loadingUtils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
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
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LoadingActivity : AppCompatActivity() {

    @Inject lateinit var syncManager: FirebaseSyncManager

    // 1. INJECT THE REPOSITORY
    @Inject lateinit var weatherRepository: WeatherRepository

    private lateinit var seekBar: SeekBar
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading)

        seekBar = findViewById(R.id.loadingSeekBar)
        statusText = findViewById(R.id.tvStatus)

        startDataSync()
    }

    private fun startDataSync() {
        lifecycleScope.launch(Dispatchers.IO) {

            updateProgress(10, "Connecting to server...")
            delay(300)

            updateProgress(30, "Syncing User Profile...")
            syncManager.syncUsers()

            updateProgress(50, "Downloading Crop Data...")
            syncManager.syncCrops()

            updateProgress(70, "Checking Weather...")

            val hasPermission = ContextCompat.checkSelfPermission(
                this@LoadingActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    fetchWeatherSync()
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private suspend fun fetchWeatherSync() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val location = fusedLocationClient.lastLocation.await() ?: return

        var locName = "Unknown"
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            locName = if (!addresses.isNullOrEmpty()) addresses[0].locality ?: "Unknown" else "Unknown"
        } catch (e: Exception) {
            Log.e("LoadingActivity", "Geocoder failed: ${e.message}")
        }

        val apiKey = BuildConfig.WEATHER_API_KEY
        val response = RetrofitClient.instance.getForecastByCoordinates(location.latitude, location.longitude, apiKey).awaitResponse()

        if (response.isSuccessful) {
            val forecasts = response.body()?.list
            if (forecasts != null) {
                val upcomingForecasts = forecasts.take(8)
                val format = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                val dateText = format.format(Date())

                weatherRepository.saveWeatherData(upcomingForecasts, locName, dateText)

                Log.d("LoadingActivity", "Weather downloaded and saved offline!")
            }
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