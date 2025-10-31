package com.example.farmforward.roomDatabase

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "crop_table", indices = [Index(value = ["userId"])])
data class CropEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val cropName: String,
    val area: Double,
    val expectedYield: Double,
    val date: Long,
    val maxdate: Long?,
    val mindate: Long?,
    val soilType: String? = null,
    val irrigationLevel: String? = null,
    val plantDensity: String? = null,
    val fertilizerUsed: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
