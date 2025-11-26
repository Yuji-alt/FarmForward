package com.example.farmforward.appActivity.mainActivity.home

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.roomDatabase.CropEntity
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.example.farmforward.utils.weatherUtils.WeatherResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class HomeFragmentController @Inject constructor(
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
    private val WEATHER_FETCH_COOLDOWN = 10 * 60 * 1000L

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
        weatherRepository.loadCachedData()
        if (!weatherRepository.cachedForecasts.isNullOrEmpty()) {
            view?.showWeatherContainer(true)
            view?.setLocationText(weatherRepository.cachedLocationName ?: "Unknown")
            view?.setWeatherDateText(weatherRepository.cachedDateText ?: "Today")
            view?.displayForecast(weatherRepository.cachedForecasts!!)
        } else {
            val now = System.currentTimeMillis()
            if (now - lastWeatherFetchTime > WEATHER_FETCH_COOLDOWN) {
                lastWeatherFetchTime = now
                fetchWeatherByLocation()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        filterAndDisplayCrops(query)
    }

    fun onPermissionGranted() {
        Log.d("HomeFragment", "Permission was granted! Re-fetching weather.")
        fetchWeatherByLocation()
    }

    fun onPermissionDenied() {
        Log.d("HomeFragment", "Permission was denied.")
        view?.setLocationText(context.getString(R.string.permission_needed))
        view?.setWeatherDateText("---")
    }

    private fun filterAndDisplayCrops(query: String) {
        val filteredCrops = if (query.isEmpty()) {
            allCrops
        } else {
            allCrops.filter { crop -> crop.cropName.contains(query, ignoreCase = true) }
        }
        view?.displayCrops(filteredCrops)
    }

    private fun fetchWeatherByLocation() {
        val activity = view?.getMainActivity() ?: return
        val mainController = activity.controller

        view?.showWeatherContainer(true)
        view?.setLocationText(context.getString(R.string.location_loading))
        view?.setWeatherDateText(context.getString(R.string.loading))

        mainController.checkAndRequestLocationPermission(
            activity,
            onPermissionGranted = {
                mainController.fetchCurrentLocation(activity) { lat, lon ->
                    scopeOwner?.launch(Dispatchers.IO) {
                        val locationName = getLocationName(lat, lon)
                        withContext(Dispatchers.Main) {
                            view?.setLocationText(locationName)
                        }
                        fetchWeatherForecast(lat, lon, locationName)
                    }
                }
            },
            onPermissionDenied = { onPermissionDenied() }
        )
    }

    private fun fetchWeatherForecast(lat: Double, lon: Double, locationName: String) {
        val apiKey = BuildConfig.WEATHER_API_KEY

        RetrofitClient.instance.getForecastByCoordinates(lat, lon, apiKey)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    view?.showWeatherContainer(true)

                    if (response.isSuccessful) {
                        val allForecasts = response.body()?.list ?: return
                        view?.setWeatherDateText(getWeatherDay())
                        val upcomingForecasts = allForecasts.take(8)

                        weatherRepository.saveWeatherData(upcomingForecasts, locationName, getWeatherDay())

                        view?.displayForecast(upcomingForecasts)
                    } else {
                        Log.d("WeatherAPI", "API Error: ${response.code()}")
                        view?.showToast("Failed to load weather.", isError = true)
                        view?.setWeatherDateText("Error")
                        view?.setLocationText("---")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    view?.showWeatherContainer(true)
                    Log.e("WeatherAPI", "Network Error: ${t.localizedMessage}")
                    weatherRepository.loadCachedData()

                    if(!weatherRepository.cachedForecasts.isNullOrEmpty()) {
                        view?.showToast("Offline Mode: Showing cached weather.", isError = false)

                        view?.setLocationText(weatherRepository.cachedLocationName ?: "Unknown")
                        view?.setWeatherDateText(weatherRepository.cachedDateText ?: "")
                        view?.displayForecast(weatherRepository.cachedForecasts!!)
                    } else {
                        view?.showToast("Network error.", isError = true)
                        view?.setWeatherDateText(context.getString(R.string.network_failed))
                    }
                }
            })
    }
    private fun getLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
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