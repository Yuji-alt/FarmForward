package com.example.farmforward.database.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmforward.R
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.dataclass.CropFormDraft
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.utils.weatherUtils.WeatherResponse
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CropViewModel @Inject constructor(
    application: Application,
    private val syncManager: FirebaseSyncManager
) : AndroidViewModel(application) {

    private val cropDao = AppDatabase.getDatabase(application).cropDao()

    private val _cropData = MutableLiveData<CropEntity?>()
    val cropData: LiveData<CropEntity?> get() = _cropData

    private val _pickedLocation = MutableLiveData<LatLng?>()
    val pickedLocation: LiveData<LatLng?> get() = _pickedLocation

    var formDraft: CropFormDraft? = null
    var isMapPickerMode = false
    var cropToEdit: CropEntity? = null
    var lastSourceId: Int = R.id.nav_home
    var tempDate: Long? = null
    private val weatherCache = mutableMapOf<String, WeatherCacheItem>()
    data class WeatherCacheItem(val response: WeatherResponse, val timestamp: Long)
    private val _cropToFocus = MutableLiveData<CropEntity?>()  // make private
    val cropToFocus: LiveData<CropEntity?> get() = _cropToFocus

    fun setCropToFocus(crop: CropEntity?) {
        _cropToFocus.value = crop
    }

    fun clearCropToFocus() {
        _cropToFocus.value = null
    }

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

    fun pickLocation(lat: Double, lng: Double) {
        _pickedLocation.value = LatLng(lat, lng)
    }

    fun clearPickedLocation() {
        _pickedLocation.value = null
    }
    fun saveNewCrop(
        userId: Int, cropName: String, area: Double, roundedYield: Double, dateToPlant: Long,
        minDateMillis: Long?, maxDateMillis: Long?, soilType: String?, irrigationLevel: String?,
        plantDensity: String?, fertilizerUsed: String?, isSynced: Int, weatherCondition: String?,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        region: String = "",
        locality: String = ""
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
            weatherCondition = weatherCondition,
            lastUpdated = System.currentTimeMillis(),
            isSynced = isSynced,
            latitude = latitude,
            longitude = longitude,
            region = region,
            locality = locality
        )
        viewModelScope.launch(Dispatchers.IO) {
            cropDao.insertCrop(newCrop)
            _cropData.postValue(newCrop)
            syncManager.syncCrops()
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
        weatherCondition: String?,
        isSynced: Int,
        latitude: Double,
        longitude: Double,
        region: String = "",
        locality: String = ""
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
            weatherCondition = weatherCondition,
            lastUpdated = System.currentTimeMillis(),
            isSynced = isSynced,
            latitude = latitude,
            longitude = longitude,
            region = region,
            locality = locality
        )
        viewModelScope.launch(Dispatchers.IO) {
            cropDao.updateCrop(updatedCrop)
            _cropData.postValue(updatedCrop)
            syncManager.syncCrops()
        }
    }

    fun deleteCrop(cropId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            cropDao.softDeleteCrop(cropId)
            syncManager.syncCrops()
        }
    }

    fun clearDraft() {
        formDraft = null
    }

    fun harvestCrop(crop: CropEntity, harvestDate: Long) {
        val updatedCrop = crop.copy(
            harvestedDate = harvestDate,
            lastUpdated = System.currentTimeMillis(),
            isSynced = 0
        )

        viewModelScope.launch(Dispatchers.IO) {
            cropDao.updateCrop(updatedCrop)
            _cropData.postValue(updatedCrop)
            syncManager.syncCrops()
        }
    }
}