package com.example.farmforward.appActivity.mainActivity.otherFragment.CropDetails

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.CropImageHelper
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.example.farmforward.utils.weatherUtils.WeatherResponse
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
import kotlin.math.abs

class CropDetailsController @Inject constructor() {

    private var view: CropDetailsView? = null
    @Inject lateinit var weatherRepository: WeatherRepository
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun bindView(view: CropDetailsView) {
        this.view = view
    }


    fun onEditClicked(viewModel: CropViewModel) {
        val currentCrop = viewModel.cropData.value
        if (currentCrop != null) {
            viewModel.cropToEdit = currentCrop
            view?.navigateToEdit(currentCrop)
        }
    }

    fun onViewOnMapClicked(viewModel: CropViewModel) {
        val currentCrop = viewModel.cropData.value
        if (currentCrop != null && currentCrop.latitude != 0.0) {
            viewModel.cropToFocus = currentCrop
            viewModel.isMapPickerMode = false
            view?.navigateToMap(currentCrop)
        }
    }

    fun setupObserver(lifecycleOwner: LifecycleOwner, viewModel: CropViewModel) {
        viewModel.cropData.observe(lifecycleOwner) { crop ->
            if (crop == null || crop.cropName.isBlank()) {
                view?.showEmptyState()
                view?.showMapButton(false)
                view?.showHarvestButton(false)
            } else {
                view?.setCropName(crop.cropName)
                view?.setArea("${crop.area} sq. meters")
                view?.setPlantedDate(dateFormat.format(crop.date))

                val isHarvested = crop.harvestedDate != null
                view?.showHarvestButton(!isHarvested)
                view?.setCropImage(CropImageHelper.getImageRes(crop.cropName))
                view?.setCropImageTint(R.color.moss_green)
                view?.setYield("${crop.expectedYield} kg")
                view?.setSoil(crop.soilType ?: "N/A")
                view?.setIrrigation(crop.irrigationLevel ?: "N/A")
                view?.setDensity(crop.plantDensity ?: "N/A")
                view?.setFertilizer(crop.fertilizerUsed ?: "N/A")
                view?.setLocation(crop.region, crop.locality)

                if (crop.latitude != 0.0 && crop.longitude != 0.0) {
                    view?.showMapButton(true)
                } else {
                    view?.showMapButton(false)
                }
                fetchWeatherForCrop(crop, viewModel)
            }
        }
    }

    fun onHarvestClicked(viewModel: CropViewModel) {
        val currentCrop = viewModel.cropData.value ?: return
        val minDate = currentCrop.mindate ?: 0L
        val today = System.currentTimeMillis()
        if (today >= minDate) {
            viewModel.harvestCrop(currentCrop, today)
            view?.navigateToGarden()
        } else {
            val dateStr = dateFormat.format(Date(minDate))
            view?.showError("Crop not ready. Estimated harvest date: $dateStr")
        }
    }

    private fun fetchWeatherForCrop(crop: CropEntity, viewModel: CropViewModel) {
        if (crop.latitude != 0.0 && crop.longitude != 0.0) {
            val cachedData = viewModel.getCachedWeather(crop.latitude, crop.longitude)

            if (cachedData != null) {
                displayWeatherFromResponse(cachedData, isCached = true)
            } else {
                view?.setWeather("Loading local weather...")
                val apiKey = BuildConfig.WEATHER_API_KEY
                viewModel.viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.instance.getForecastByCoordinates(crop.latitude, crop.longitude, apiKey)

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful && response.body() != null) {
                                viewModel.cacheWeather(crop.latitude, crop.longitude, response.body()!!)
                                displayWeatherFromResponse(response.body()!!, isCached = false)
                            } else {
                                updateCurrentDeviceWeather()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            updateCurrentDeviceWeather()
                        }
                    }
                }
            }
        } else {
            updateCurrentDeviceWeather()
        }
    }

    fun onDeleteClicked(viewModel: CropViewModel) {
        val crop = viewModel.cropData.value ?: return
        view?.showDeleteConfirmation("Are you sure you want to delete this crop?") {
            viewModel.deleteCrop(crop.id)
            view?.navigateToGarden()
        }
    }

    private fun displayWeatherFromResponse(response: WeatherResponse, isCached: Boolean) {
        val forecast = response.list.firstOrNull()
        if (forecast != null) {
            val condition = forecast.weather.firstOrNull()?.main ?: "Clear"
            val tempVal = forecast.main.temp
            val temp = String.format("%.1f°C", tempVal)
            val sourceTag = if (isCached) " (Saved)" else ""
            view?.setWeather("$condition, $temp$sourceTag")
        }
    }

    private fun updateCurrentDeviceWeather() {
        weatherRepository.loadCachedData()
        val forecasts = weatherRepository.cachedForecasts
        if (!forecasts.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val closest = forecasts.minByOrNull { abs((it.dt * 1000) - now) }
            if (closest != null) {
                val condition = closest.weather.firstOrNull()?.main ?: "Unknown"
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeString = timeFormat.format(Date(closest.dt * 1000))
                view?.setWeather("$condition $timeString")
            } else {
                view?.setWeather("Weather Unavailable")
            }
        } else {
            view?.setWeather("Offline / No Data")
        }
    }

    fun onDestroy() {
        view = null
    }
}