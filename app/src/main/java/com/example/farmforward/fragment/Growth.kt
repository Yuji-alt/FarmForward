package com.example.farmforward.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.farmforward.R
import com.example.farmforward.fragmentController.GrowthController
import java.text.SimpleDateFormat
import java.util.*

class GrowthFragment : Fragment() {

    private lateinit var controller: GrowthController
    private lateinit var tvCropName: TextView
    private lateinit var tvArea: TextView
    private lateinit var plantedDate: TextView
    private lateinit var minHarvest: TextView
    private lateinit var maxHarvest: TextView
    private lateinit var harvestYield: TextView
    private lateinit var imgCrop: ImageView

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_growth, container, false)

        controller = GrowthController(requireContext())

        tvCropName = view.findViewById(R.id.etCropName)
        tvArea = view.findViewById(R.id.tvArea)
        plantedDate = view.findViewById(R.id.plantedDate)
        minHarvest = view.findViewById(R.id.minHarvest)
        maxHarvest = view.findViewById(R.id.maxHarvest)
        harvestYield = view.findViewById(R.id.harvestYield)
        imgCrop = view.findViewById(R.id.etDescription)

        // Get crop details from arguments
        val cropName = arguments?.getString("cropName") ?: "Unknown Crop"
        val area = arguments?.getDouble("area") ?: 0.0
        val expectedYield = arguments?.getDouble("expectedYield") ?: 0.0
        val datePlanted = arguments?.getLong("datePlanted") ?: System.currentTimeMillis()
        val minHarvestDate = arguments?.getLong("minHarvestDate")
        val maxHarvestDate = arguments?.getLong("maxHarvestDate")

        controller.displayCropDetails(
            cropName,
            area,
            expectedYield,
            datePlanted,
            minHarvestDate,
            maxHarvestDate,
            tvCropName,
            tvArea,
            plantedDate,
            minHarvest,
            maxHarvest,
            harvestYield,
            imgCrop
        )

        return view
    }
}
