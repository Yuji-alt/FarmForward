package com.example.farmforward.database.roomDatabase

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "crop_table", indices = [Index(value = ["userId"])])
data class CropEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0,
    val cropName: String = "",
    val area: Double = 0.0,
    val expectedYield: Double = 0.00,
    val date: Long = 0L,
    val maxdate: Long? = null,
    val mindate: Long? = null,
    val soilType: String? = null,
    val irrigationLevel: String? = null,
    val plantDensity: String? = null,
    val fertilizerUsed: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)
