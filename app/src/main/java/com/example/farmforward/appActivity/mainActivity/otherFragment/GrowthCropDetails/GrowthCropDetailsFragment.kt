package com.example.farmforward.appActivity.mainActivity.growth

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.mainActivity.otherFragment.GrowthCropDetails.GrowthCropDetailsController
import com.example.farmforward.appActivity.mainActivity.otherFragment.GrowthCropDetails.GrowthCropDetailsView
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GrowthCropDetailsFragment : Fragment(), GrowthCropDetailsView {

    @Inject lateinit var controller: GrowthCropDetailsController
    private lateinit var cropViewModel: CropViewModel

    private lateinit var tvCropName: TextView
    private lateinit var imgCrop: ImageView
    private lateinit var tvGrowthPercent: TextView
    private lateinit var progressGrowth: SeekBar
    private lateinit var tvDatePlanted: TextView
    private lateinit var tvEstimatedHarvest: TextView
    private lateinit var tvGrowthStage: TextView
    private lateinit var tvGrowthDescription: TextView
    private lateinit var btnHarvest: Button
    private lateinit var btnViewOnMap: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.growth_crop_details, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        tvCropName = view.findViewById(R.id.tvCropName)
        imgCrop = view.findViewById(R.id.imgCrop)
        tvGrowthPercent = view.findViewById(R.id.tvGrowthPercent)
        progressGrowth = view.findViewById(R.id.progressGrowth)
        tvDatePlanted = view.findViewById(R.id.tvDatePlanted)
        tvEstimatedHarvest = view.findViewById(R.id.tvEstimatedHarvest)
        tvGrowthStage = view.findViewById(R.id.tvGrowthStage)
        tvGrowthDescription = view.findViewById(R.id.tvGrowthDescription)
        btnHarvest = view.findViewById(R.id.btnHarvest)
        btnViewOnMap = view.findViewById(R.id.btnViewOnMap)
        val btnCloseNav = view.findViewById<ImageButton>(R.id.btn_close_nav)
        progressGrowth.setOnTouchListener { _, _ -> true }

        controller.bindView(this)
        controller.setupObserver(viewLifecycleOwner, cropViewModel)

        btnCloseNav.setOnClickListener { controller.onBackClicked(cropViewModel) }
        btnHarvest.setOnClickListener { controller.onHarvestClicked(cropViewModel) }
        btnViewOnMap.setOnClickListener {
            val crop = cropViewModel.cropData.value
            if (crop != null) controller.onViewOnMapClicked(cropViewModel, crop)
        }


        return view
    }

    override fun setCropName(name: String) { tvCropName.text = name }

    override fun setCropImage(resourceId: Int) { imgCrop.setImageResource(resourceId) }

    override fun setCropImageTint(colorRes: Int) {
        val color = ContextCompat.getColor(requireContext(), colorRes)
        imgCrop.setColorFilter(color)
    }

    override fun setGrowthDetails(percent: Int, stage: String, description: String) {
        tvGrowthPercent.text = "Growth: $percent%"
        progressGrowth.progress = percent
        tvGrowthStage.text = "Growth Stage: $stage"
        tvGrowthDescription.text = "Description: $description"
    }

    override fun setPlantedDate(date: String) {
        tvDatePlanted.text = "Date Planted: $date"
    }

    override fun setEstimatedHarvest(date: String) {
        tvEstimatedHarvest.text = "Estimated Harvest: $date"
    }

    override fun showEmptyState() {
        tvCropName.text = "No Crop"
        btnHarvest.visibility = View.GONE
        btnViewOnMap.visibility = View.GONE
    }

    override fun showHarvestButton(isVisible: Boolean) {
        btnHarvest.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun showMapButton(isVisible: Boolean) {
        btnViewOnMap.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun navigateBack(destinationId: Int) {
        // Direct switch to bypass controller checks
        (activity as? MainActivity)?.switchFragment(destinationId)
    }

    override fun navigateToMap(crop: CropEntity) {
        (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_map)
    }

    override fun navigateToGrowth() {
        (activity as? MainActivity)?.switchFragment(R.id.nav_growth)
    }

    override fun showHarvestConfirmation(message: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Harvest")
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }

    override fun getFragmentContext(): Context = requireContext()
    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }
}