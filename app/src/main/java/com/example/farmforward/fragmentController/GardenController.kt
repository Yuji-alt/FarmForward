package com.example.farmforward.fragmentController

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.farmforward.R
import com.example.farmforward.roomDatabase.CropEntity

class GardenController(private val context: Context, private val container: LinearLayout) {

    @SuppressLint("SetTextI18n")
    fun displayCrops(crops: List<CropEntity>, onItemClick: (CropEntity) -> Unit) {
        container.removeAllViews()

        val inflater = LayoutInflater.from(context)
        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.garden_frame, container, false)

            val tvCropName = itemView.findViewById<TextView>(R.id.tvCropName)
            val tvCropDetails = itemView.findViewById<TextView>(R.id.tvCropDetails)
            val imgCrop = itemView.findViewById<ImageView>(R.id.imgCrop)

            tvCropName.text = crop.cropName
            val details = buildString {
                appendLine("Expected yield: ${String.format("%.2f", crop.expectedYield)} kg")
                crop.soilType?.let { appendLine("Soil Type: $it") }
                crop.irrigationLevel?.let { appendLine("Irrigation: $it") }
                crop.plantDensity?.let { appendLine("Density: $it") }
                crop.fertilizerUsed?.let { appendLine("Fertilizer: $it") }
            }
            tvCropDetails.text = details.trim()

            itemView.setOnClickListener {
                onItemClick(crop)
            }

            container.addView(itemView)
        }
    }
}
