package com.example.farmforward.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.roomDatabase.CropEntity


class CropViewModel(application: Application) : AndroidViewModel(application) {

    private val cropDao = AppDatabase.getDatabase(application).cropDao()

    private val _cropData = MutableLiveData<CropEntity?>()
    val cropData: LiveData<CropEntity?> get() = _cropData
    fun viewCropDetails(crop: CropEntity) {
        _cropData.postValue(crop)
    }
    suspend fun saveNewCrop(
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
        fertilizerUsed: String?
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
            lastUpdated = System.currentTimeMillis()
        )

        cropDao.insertCrop(newCrop)

        _cropData.postValue(newCrop)
    }
}