package com.example.farmforward.fragmentController

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.example.farmforward.R
import java.text.SimpleDateFormat
import java.util.*

class GrowthController(private val context: Context) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun displayCropDetails(
        cropName: String,
        area: Double,
        expectedYield: Double,
        datePlanted: Long,
        minHarvestMillis: Long?,
        maxHarvestMillis: Long?,
        soilType: String?,
        irrigationLevel: String?,
        plantDensity: String?,
        fertilizerUsed: String?,
        tvCropName: TextView,
        tvArea: TextView,
        plantedDate: TextView,
        minHarvest: TextView,
        maxHarvest: TextView,
        harvestYield: TextView,
        tvSoilType: TextView,
        tvIrrigation: TextView,
        tvDensity: TextView,
        tvFertilizer: TextView,
        imgCrop: ImageView
    ) {
        tvCropName.text = cropName
        tvArea.text = "$area sq. meters"
        harvestYield.text = "${String.format("%.2f", expectedYield)} kg"

        plantedDate.text = dateFormat.format(Date(datePlanted))
        minHarvest.text = minHarvestMillis?.let { "Min: ${dateFormat.format(Date(it))}" } ?: "N/A"
        maxHarvest.text = maxHarvestMillis?.let { "Max: ${dateFormat.format(Date(it))}" } ?: "N/A"

        tvSoilType.text = soilType ?: "N/A"
        tvIrrigation.text = irrigationLevel ?: "N/A"
        tvDensity.text = plantDensity ?: "N/A"
        tvFertilizer.text = fertilizerUsed ?: "N/A"
        imgCrop.setImageResource(getCropImage(cropName))
    }

    private fun getCropImage(cropName: String): Int {
        val lower = cropName.lowercase()
        return when {
            else -> R.drawable.image_border
        }
    }
}
