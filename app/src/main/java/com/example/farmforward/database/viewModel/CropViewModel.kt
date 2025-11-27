package com.example.farmforward.database.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.roomDatabase.CropEntity
import com.example.farmforward.utils.weatherUtils.WeatherResponse // Ensure this is imported
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CropViewModel(application: Application) : AndroidViewModel(application) {

    private val cropDao = AppDatabase.getDatabase(application).cropDao()

    private val _cropData = MutableLiveData<CropEntity?>()
    val cropData: LiveData<CropEntity?> get() = _cropData

    private val _pickedLocation = MutableLiveData<LatLng?>()
    val pickedLocation: LiveData<LatLng?> get() = _pickedLocation
    var formDraft: CropFormDraft? = null
    var isMapPickerMode = false
    var cropToEdit: CropEntity? = null
    private val weatherCache = mutableMapOf<String, WeatherCacheItem>()

    data class WeatherCacheItem(val response: WeatherResponse, val timestamp: Long)

    var mapFocusLocation: LatLng? = null

    fun getCachedWeather(lat: Double, lng: Double): WeatherResponse? {
        val key = "${lat}_${lng}"
        val item = weatherCache[key] ?: return null
        val tenMinutes = 10 * 60 * 1000
        val now = System.currentTimeMillis()

        return if (now - item.timestamp < tenMinutes) {
            item.response
        } else {
            weatherCache.remove(key)
            null
        }
    }
    fun cacheWeather(lat: Double, lng: Double, response: WeatherResponse) {
        val key = "${lat}_${lng}"
        weatherCache[key] = WeatherCacheItem(response, System.currentTimeMillis())
    }
    fun viewCropDetails(crop: CropEntity) {
        _cropData.value = crop
    }

    fun setPickedLocation(lat: Double, lng: Double) {
        _pickedLocation.value = LatLng(lat, lng)
    }

    fun clearPickedLocation() {
        _pickedLocation.value = null
    }

    fun saveNewCrop(
        userId: Int,
        cropName: String,
        area: Double,
        roundedYield: Double,
        dateToPlant: Long,
        minDateMillis: Long?,
        maxDateMillis: Long?,
        soilType: String?,
        irrigationLevel: String?,
        plantDensity: String?,
        fertilizerUsed: String?,
        isSynced: Int,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ) {
        val newCrop = CropEntity(
            userId = userId,
            cropName = cropName,
            area = area,
            expectedYield = roundedYield,
            date = dateToPlant,
            mindate = minDateMillis,
            maxdate = maxDateMillis,
            soilType = soilType,
            irrigationLevel = irrigationLevel,
            plantDensity = plantDensity,
            fertilizerUsed = fertilizerUsed,
            lastUpdated = System.currentTimeMillis(),
            isSynced = isSynced,
            latitude = latitude,
            longitude = longitude
        )

        viewModelScope.launch(Dispatchers.IO) {
            cropDao.insertCrop(newCrop)
            _cropData.postValue(newCrop)
        }
    }

    fun updateCrop(
        originalCrop: CropEntity,
        area: Double,
        roundedYield: Double,
        dateToPlant: Long,
        minDateMillis: Long?,
        maxDateMillis: Long?,
        soilType: String?,
        irrigationLevel: String?,
        plantDensity: String?,
        fertilizerUsed: String?,
        isSynced: Int,
        latitude: Double,
        longitude: Double
    ) {
        val updatedCrop = originalCrop.copy(
            area = area,
            expectedYield = roundedYield,
            date = dateToPlant,
            mindate = minDateMillis,
            maxdate = maxDateMillis,
            soilType = soilType,
            irrigationLevel = irrigationLevel,
            plantDensity = plantDensity,
            fertilizerUsed = fertilizerUsed,
            lastUpdated = System.currentTimeMillis(),
            isSynced = isSynced,
            latitude = latitude,
            longitude = longitude
        )

        viewModelScope.launch(Dispatchers.IO) {
            cropDao.updateCrop(updatedCrop)
            _cropData.postValue(updatedCrop)
        }
    }
    fun clearDraft() {
        formDraft = null
    }
}

data class CropFormDraft(
    val name: String,
    val area: String,
    val soil: String,
    val irrigation: String,
    val density: String,
    val fertilizer: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)