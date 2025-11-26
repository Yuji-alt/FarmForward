package com.example.farmforward.appActivity.mainActivity.growth

import androidx.lifecycle.LifecycleOwner
import com.example.farmforward.R
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.example.farmforward.database.viewModel.CropViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class GrowthController @Inject constructor() {

    private var view: GrowthView? = null
    @Inject lateinit var weatherRepository: WeatherRepository
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun bindView(view: GrowthView) {
        this.view = view
    }

    fun setupObserver(lifecycleOwner: LifecycleOwner, viewModel: CropViewModel) {

        viewModel.cropData.observe(lifecycleOwner) { crop ->
            if (crop == null || crop.cropName.isBlank()) {
                view?.showEmptyState()
            } else {
                val plantedDateText = dateFormat.format(crop.date)
                val minHarvestText = crop.mindate?.let { dateFormat.format(it) } ?: "N/A"
                val maxHarvestText = crop.maxdate?.let { dateFormat.format(it) } ?: "N/A"
                val imageResId = getCropImage(crop.cropName)

                view?.setCropName(crop.cropName)
                view?.setArea("${crop.area} sq. meters")
                view?.setPlantedDate(plantedDateText)
                view?.setMinHarvest(minHarvestText)
                view?.setMaxHarvest(maxHarvestText)
                view?.setYield("${crop.expectedYield} kg")
                view?.setSoil(crop.soilType ?: "N/A")
                view?.setIrrigation(crop.irrigationLevel ?: "N/A")
                view?.setDensity(crop.plantDensity ?: "N/A")
                view?.setFertilizer(crop.fertilizerUsed ?: "N/A")
                view?.setCropImage(imageResId)
            }
        }
        updateCurrentWeather()
    }

    private fun updateCurrentWeather() {
        weatherRepository.loadCachedData()

        val forecasts = weatherRepository.cachedForecasts

        if (!forecasts.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val closestForecast = forecasts.minByOrNull { abs((it.dt * 1000) - now) }

            if (closestForecast != null && !closestForecast.weather.isNullOrEmpty()) {
                val mainCondition = closestForecast.weather[0].main
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeString = timeFormat.format(Date(closestForecast.dt * 1000))

                view?.setWeather("$mainCondition @ $timeString")
            } else {
                view?.setWeather("Weather Unavailable")
            }
        } else {
            view?.setWeather("Offline / No Data")
        }
    }

    private fun getCropImage(cropName: String): Int {
        val lower = cropName.lowercase()
        return when {
            lower.contains("corn") -> R.drawable.ic_settings
            lower.contains("rice") -> R.drawable.ic_settings
            else -> R.drawable.image_border
        }
    }

    fun onDestroy() {
        view = null
    }
}