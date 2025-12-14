package com.example.farmforward.appActivity.mainActivity.home

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.farmforward.BuildConfig
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class HomeController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val sessionManager: SessionManager,
    private val weatherRepository: WeatherRepository
) {
    private var view: HomeView? = null
    private var scopeOwner: LifecycleCoroutineScope? = null

    private var allCrops: List<CropEntity> = emptyList()
    private var currentSearchQuery: String = ""

    private var lastWeatherFetchTime: Long = 0L
    private val cropsObserver = Observer<List<CropEntity>> { crops ->
        allCrops = crops
        filterAndDisplayCrops(currentSearchQuery)
    }

    fun bindView(view: HomeView, scope: LifecycleCoroutineScope) {
        this.view = view
        this.scopeOwner = scope
    }

    fun setupObserver(lifecycleOwner: LifecycleOwner) {
        val userId = sessionManager.getUserId()
        if (userId == -1) return
        db.cropDao().getCropsForUser(userId).observe(lifecycleOwner, cropsObserver)
    }

    fun onViewResumed() {
        val activity = view?.getMainActivity() ?: return
        val mainController = activity.controller

        // 1. Check Permission First
        if (!mainController.hasLocationPermission()) {
            onPermissionDenied()
            return
        }

        // 2. Load and Show Cached Data immediately
        weatherRepository.loadCachedData()
        displayCachedData()
        checkLocationAndRefreshIfNeeded()
    }

    private fun displayCachedData() {
        if (!weatherRepository.cachedForecasts.isNullOrEmpty()) {
            view?.showWeatherContainer(true)
            view?.setLocationText(weatherRepository.cachedLocationName ?: "Unknown")
            view?.setWeatherDateText(weatherRepository.cachedDateText ?: "Today")
            view?.displayForecast(weatherRepository.cachedForecasts!!)
        }
    }

    private fun checkLocationAndRefreshIfNeeded() {
        val activity = view?.getMainActivity() ?: return
        val mainController = activity.controller

        if (mainController.hasLocationPermission() && NetworkUtils.isNetworkAvailable(context)) {
            mainController.fetchCurrentLocation(activity) { lat, lon ->
                if (lat == 0.0 && lon == 0.0) {
                    scopeOwner?.launch(Dispatchers.Main) {
                        val cachedName = weatherRepository.cachedLocationName

                        if (!cachedName.isNullOrEmpty()) {
                            view?.setLocationText("$cachedName - Old location(Enable GPS to update)")
                        } else {
                            view?.setLocationText("Location Off")
                            view?.setWeatherDateText("Enable GPS to update")
                        }
                    }
                    return@fetchCurrentLocation
                }

                scopeOwner?.launch(Dispatchers.IO) {
                    val currentLocality = getLocationName(lat, lon)
                    val cachedLocality = weatherRepository.cachedLocationName ?: ""

                    val locationChanged = !currentLocality.equals(cachedLocality, ignoreCase = true)
                    var isForecastExpired = false
                    val firstForecast = weatherRepository.cachedForecasts?.firstOrNull()

                    if (firstForecast != null) {
                        val forecastTimeMillis = firstForecast.dt * 1000L
                        val threeHoursMillis = 3 * 60 * 60 * 1000L
                        if (System.currentTimeMillis() > (forecastTimeMillis + threeHoursMillis)) {
                            isForecastExpired = true
                        }
                    } else {
                        isForecastExpired = true
                    }
                    if (locationChanged || isForecastExpired) {
                        Log.d("HomeController", "Refreshing weather. LocationChanged: $locationChanged")
                        withContext(Dispatchers.Main) {
                            view?.setLocationText(currentLocality)
                            view?.setWeatherDateText("Updating...")
                        }
                        fetchWeatherForecast(lat, lon, currentLocality)
                    }
                }
            }
        } else {
            if (!mainController.hasLocationPermission()) {
                onPermissionDenied()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        filterAndDisplayCrops(query)
    }

    fun onPermissionGranted() {
        checkLocationAndRefreshIfNeeded()
    }

    fun onPermissionDenied() {
        if (weatherRepository.cachedForecasts.isNullOrEmpty()) {
            view?.setLocationText("Permission Needed")
            view?.setWeatherDateText("Tap Settings to enable")
        } else {
            displayCachedData()
        }
    }

    private fun filterAndDisplayCrops(query: String) {
        val filteredCrops = if (query.isEmpty()) {
            allCrops
        } else {
            allCrops.filter { crop -> crop.cropName.contains(query, ignoreCase = true) }
        }
        view?.displayCrops(filteredCrops)
    }

    private fun fetchWeatherForecast(lat: Double, lon: Double, locationName: String) {
        lastWeatherFetchTime = System.currentTimeMillis()
        val apiKey = BuildConfig.WEATHER_API_KEY
        scopeOwner?.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getForecastByCoordinates(lat, lon, apiKey)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val allForecasts = response.body()!!.list
                        val now = System.currentTimeMillis()
                        val futureForecasts = allForecasts.filter { item ->
                            val itemTime = item.dt * 1000L
                            itemTime >= (now - 3600000)
                        }
                        val displayList = futureForecasts.take(9)

                        weatherRepository.saveWeatherData(
                            displayList,
                            locationName,
                            getWeatherDay(),
                            lat,
                            lon
                        )
                        view?.showWeatherContainer(true)
                        view?.setLocationText(locationName)
                        view?.setWeatherDateText(getWeatherDay())
                        view?.displayForecast(displayList)
                    } else {
                        displayCachedData()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    displayCachedData()
                }
            }
        }
    }

    private fun getLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.adminArea ?: "Unknown Location"
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown Location"
        }
    }
    private fun getWeatherDay(): String {
        val format = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        return format.format(Date())
    }

    fun onDestroy() {
        if (sessionManager.getUserId() != -1) {
            db.cropDao().getCropsForUser(sessionManager.getUserId()).removeObserver(cropsObserver)
        }
        view = null
        scopeOwner = null
    }
}