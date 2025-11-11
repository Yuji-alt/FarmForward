package com.example.farmforward

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.farmforward.firebase.FirebaseCropRepository
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.roomDatabase.CropEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CropViewModel(application: Application) : AndroidViewModel(application) {

    private val cropDao = AppDatabase.getDatabase(application).cropDao()

    private val _cropData = MutableLiveData<CropEntity?>()
    val cropData: LiveData<CropEntity?> get() = _cropData
    fun viewCropDetails(crop: CropEntity) {
        _cropData.value = crop
    }

    fun saveNewCrop(
        userId: Int,
        cropName: String,
        area: Double,
        expectedYield: Double,
        date: Long,
        mindate: Long?,
        maxdate: Long?,
        soilType: String?,
        irrigationLevel: String?,
        plantDensity: String?,
        fertilizerUsed: String?
    ) {
        val crop = CropEntity(
            userId = userId,
            cropName = cropName,
            area = area,
            expectedYield = expectedYield,
            date = date,
            maxdate = maxdate,
            mindate = mindate,
            soilType = soilType,
            irrigationLevel = irrigationLevel,
            plantDensity = plantDensity,
            fertilizerUsed = fertilizerUsed
        )
        _cropData.value = crop

        viewModelScope.launch(Dispatchers.IO) {
            cropDao.insertCrop(crop)
            FirebaseCropRepository().insertCrop(crop)
        }
    }
}