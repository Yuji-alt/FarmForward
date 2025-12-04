package com.example.farmforward.database.roomDatabase

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.farmforward.database.CropEntity

@Dao
interface RoomCropDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrop(crop: CropEntity)

    @Query("SELECT * FROM crop_table WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getCropsForUser(userId: Int?): LiveData<List<CropEntity>>

    @Query("SELECT * FROM crop_table")
    suspend fun getAllCrops(): List<CropEntity>

    @Query("DELETE FROM crop_table WHERE id = :id")
    suspend fun deleteCropById(id: Int)
    @Query("SELECT * FROM crop_table WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    suspend fun getCropsForUserList(userId: Int): List<CropEntity>

    @Query("SELECT COUNT(*) FROM crop_table WHERE userId = :userId")
    suspend fun countUserCrops(userId: Int): Int

    @Query("SELECT * FROM crop_table WHERE userId = :userId AND isSynced = 0 ORDER BY date ASC")
    suspend fun getUnsyncedCrops(userId: Int): List<CropEntity>

    @Query("UPDATE crop_table SET isSynced = 1 WHERE id = :cropId")
    suspend fun markAsSynced(cropId: Int)

    @Update
    suspend fun updateCrop(crop: CropEntity)

    @Query("UPDATE crop_table SET isDeleted = 1, isSynced = 0 WHERE id = :cropId")
    suspend fun softDeleteCrop(cropId: Int)
    @Query("SELECT * FROM crop_table WHERE userId = :userId")

    suspend fun getAllCropsIncludeDeleted(userId: Int): List<CropEntity>
    @Query("SELECT COUNT(*) FROM crop_table WHERE userId = :userId AND harvestedDate IS NULL AND isDeleted = 0")
    suspend fun getActivePlantCount(userId: Int): Int
    @Query("SELECT * FROM crop_table WHERE userId = :userId AND harvestedDate IS NOT NULL AND isDeleted = 0 ORDER BY harvestedDate DESC")
    suspend fun getHarvestedCrops(userId: Int): List<CropEntity>

    @Query("SELECT COUNT(*) FROM crop_table WHERE userId = :userId AND harvestedDate IS NOT NULL AND isDeleted = 0")
    suspend fun getHarvestedCount(userId: Int): Int
    @Query("SELECT cropName FROM crop_table WHERE userId = :userId AND isDeleted = 0 GROUP BY cropName ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun getFavoriteCrop(userId: Int): String?

    @Query("DELETE FROM crop_table WHERE userId = :userId")
    suspend fun deleteAllCropsForUser(userId: Int)
}