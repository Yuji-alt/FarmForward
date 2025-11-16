package com.example.farmforward.appActivity.growth

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.viewModel.CropViewModel

// No changes to the class signature
class GrowthFragment : Fragment(), GrowthView {

    private lateinit var controller: GrowthController
    private lateinit var cropViewModel: CropViewModel

    // UI Elements
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
        controller = GrowthController(this)

        // Find all the views
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


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tell the controller to observe the LiveData
        controller.setupObserver()
    }


    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }


    override fun getFragmentContext(): Context = requireContext()

    override fun getViewModel(): CropViewModel = cropViewModel

    override fun setCropName(name: String) { tvCropName.text = name }
    override fun setArea(area: String) { tvArea.text = area }
    override fun setPlantedDate(date: String) { plantedDate.text = date }
    override fun setMinHarvest(date: String) { minHarvest.text = date }
    override fun setMaxHarvest(date: String) { maxHarvest.text = date }
    override fun setYield(yield: String) { harvestYield.text = yield }
    override fun setSoil(soil: String) { tvSoilType.text = soil }
    override fun setIrrigation(irrigation: String) { tvIrrigation.text = irrigation }
    override fun setDensity(density: String) { tvDensity.text = density }
    override fun setFertilizer(fertilizer: String) { tvFertilizer.text = fertilizer }

    override fun setCropImage(resourceId: Int) {
        imgCrop.setImageResource(resourceId)
    }

    override fun showEmptyState() {
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
    }
}