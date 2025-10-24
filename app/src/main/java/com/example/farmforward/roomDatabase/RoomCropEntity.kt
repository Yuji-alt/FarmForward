package com.example.farmforward.roomDatabase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Entity(tableName = "crop_table")
data class CropEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val cropName: String,
    val area: Double,
    val expectedYield: Double,
    val date: Long
)

class CropViewModel(application: Application) : AndroidViewModel(application) {

    private val cropDao = AppDatabase.getDatabase(application).cropDao()

    private val _cropData = MutableLiveData<CropEntity?>()
    val cropData: LiveData<CropEntity?> get() = _cropData

    fun setCropData(userId: Int, cropName: String, area: Double, expectedYield: Double) {
        val crop = CropEntity(
            userId = userId,
            cropName = cropName,
            area = area,
            expectedYield = expectedYield,
            date = System.currentTimeMillis()
        )

        _cropData.value = crop

        viewModelScope.launch(Dispatchers.IO) {
            cropDao.insertCrop(crop)
        }
    }
    fun getUserCrops(userId: Int): LiveData<List<CropEntity>> {
        return cropDao.getCropsForUser(userId)
    }
}
