package com.example.farmforward.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import java.text.SimpleDateFormat
import java.util.Locale

class GrowthFragment : Fragment() {

    // 1. Get the ViewModel
    private lateinit var cropViewModel: CropViewModel

    // 2. Define all your UI views
    private lateinit var tvCropName: TextView
    private lateinit var tvArea: TextView
    private lateinit var plantedDate: TextView
    private lateinit var minHarvest: TextView
    private lateinit var maxHarvest: TextView
    private lateinit var harvestYield: TextView
    private lateinit var imgCrop: ImageView
    private lateinit var tvSoilType: TextView
    private lateinit var tvIrrigation: TextView
    private lateinit var tvDensity: TextView
    private lateinit var tvFertilizer: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_growth, container, false)

        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        tvCropName = view.findViewById(R.id.tvCropName)
        tvArea = view.findViewById(R.id.tvArea)
        plantedDate = view.findViewById(R.id.plantedDate)
        minHarvest = view.findViewById(R.id.minHarvest)
        maxHarvest = view.findViewById(R.id.maxHarvest)
        harvestYield = view.findViewById(R.id.harvestYield)

        imgCrop = view.findViewById(R.id.etDescription)

        tvSoilType = view.findViewById(R.id.tvSoilType)
        tvIrrigation = view.findViewById(R.id.tvIrrigation)
        tvDensity = view.findViewById(R.id.tvDensity)
        tvFertilizer = view.findViewById(R.id.tvFertilizer)

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }


    fun refreshData() {
        val crop = cropViewModel.cropData.value

        if (crop == null || crop.cropName.isBlank()) {
            tvCropName.text = "No Crop Calculated"
            tvArea.text = "---"
            plantedDate.text = "---"
            minHarvest.text = "---"
            maxHarvest.text = "---"
            harvestYield.text = "---"
            tvSoilType.text = "---"
            tvIrrigation.text = "---"
            tvDensity.text = "---"
            tvFertilizer.text = "---"
            return
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val plantedDateText = dateFormat.format(crop.date)
        val minHarvestText = crop.mindate?.let { dateFormat.format(it) } ?: "N/A"
        val maxHarvestText = crop.maxdate?.let { dateFormat.format(it) } ?: "N/A"

        tvCropName.text = crop.cropName
        tvArea.text = "${crop.area} sq. meters"
        plantedDate.text = plantedDateText
        minHarvest.text = minHarvestText
        maxHarvest.text = maxHarvestText
        harvestYield.text = "${crop.expectedYield} kg"
        tvSoilType.text = crop.soilType
        tvIrrigation.text = crop.irrigationLevel
        tvDensity.text = crop.plantDensity
        tvFertilizer.text = crop.fertilizerUsed

        // TODO: Add logic to set imgCrop based on cropName
        // Example:
        // when (cropViewModel.cropName.lowercase()) {
        //    "corn" -> imgCrop.setImageResource(R.drawable.ic_corn)
        //    "rice" -> imgCrop.setImageResource(R.drawable.ic_rice)
        //    else -> imgCrop.setImageResource(R.drawable.ic_default_plant)
        // }
    }
}