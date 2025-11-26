package com.example.farmforward.database.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.roomDatabase.CropEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CropViewModel(application: Application) : AndroidViewModel(application) {

    private val cropDao = AppDatabase.getDatabase(application).cropDao()

    private val _cropData = MutableLiveData<CropEntity?>()
    val cropData: LiveData<CropEntity?> get() = _cropData

    fun setCropData(crop: CropEntity) {
        _cropData.postValue(crop)
    }

    fun viewCropDetails(crop: CropEntity) {
        _cropData.postValue(crop)
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
        isSynced: Int
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
            isSynced = isSynced // Save the sync status
        )

        viewModelScope.launch(Dispatchers.IO) {
            cropDao.insertCrop(newCrop)
            _cropData.postValue(newCrop)
        }
    }
}