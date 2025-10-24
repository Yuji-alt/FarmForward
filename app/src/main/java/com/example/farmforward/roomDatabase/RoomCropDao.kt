package com.example.farmforward.roomDatabase

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoomCropDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: CropEntity)

    @Query("SELECT * FROM crop_table WHERE userId = :userId ORDER BY date DESC")
    fun getCropsForUser(userId: Int): LiveData<List<CropEntity>>

    @Query("SELECT * FROM crop_table")
    suspend fun getAllCrops(): List<CropEntity>

    @Query("DELETE FROM crop_table WHERE id = :id")
    suspend fun deleteCropById(id: Int)
}