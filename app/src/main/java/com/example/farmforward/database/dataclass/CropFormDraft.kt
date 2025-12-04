package com.example.farmforward.database.dataclass

data class CropFormDraft(
    val name: String,
    val area: String,
    val soil: String,
    val irrigation: String,
    val density: String,
    val fertilizer: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)