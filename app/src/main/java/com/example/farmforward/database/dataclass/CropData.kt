package com.example.farmforward.database.dataclass

data class CropData(
    val name: String,
    val baseYield: Double,
    val minDays: Int,
    val maxDays: Int,
    val category: String
)