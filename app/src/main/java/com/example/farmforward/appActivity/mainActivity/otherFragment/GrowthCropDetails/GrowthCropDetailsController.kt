package com.example.farmforward.appActivity.mainActivity.otherFragment.GrowthCropDetails

import androidx.lifecycle.LifecycleOwner
import com.example.farmforward.R
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.CropImageHelper
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GrowthCropDetailsController @Inject constructor() {

    private var view: GrowthCropDetailsView? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun bindView(view: GrowthCropDetailsView) {
        this.view = view
    }

    fun onBackClicked(viewModel: CropViewModel) {
        val target = if (viewModel.lastSourceId != 0) viewModel.lastSourceId else R.id.nav_growth
        view?.navigateBack(target)
    }

    fun setupObserver(lifecycleOwner: LifecycleOwner, viewModel: CropViewModel) {
        viewModel.cropData.observe(lifecycleOwner) { crop ->
            if (crop == null) {
                view?.showEmptyState()
                return@observe
            }

            view?.setCropName(crop.cropName)
            view?.setCropImage(CropImageHelper.getImageRes(crop.cropName))
            view?.setCropImageTint(R.color.moss_green)

            view?.setPlantedDate(dateFormat.format(crop.date))

            val minHarvest = crop.mindate ?: 0L
            view?.setEstimatedHarvest(if (minHarvest > 0) dateFormat.format(minHarvest) else "Unknown")
            val today = System.currentTimeMillis()
            val planted = crop.date

            if (today < planted) {
                val diff = planted - today
                val daysUntil = TimeUnit.MILLISECONDS.toDays(diff) + 1

                view?.setGrowthDetails(
                    percent = 0,
                    stage = "Scheduled",
                    description = "Planting in $daysUntil days. Growth tracking will start automatically on the scheduled date."
                )
                view?.showHarvestButton(false)
            }
            else {
                val totalDuration = minHarvest - planted
                val elapsed = today - planted

                var percent = 0
                if (totalDuration > 0) {
                    percent = ((elapsed.toDouble() / totalDuration.toDouble()) * 100).toInt()
                }
                if (percent > 100) percent = 100
                if (percent < 0) percent = 0

                val (stage, desc) = when {
                    percent < 20 -> "Germination" to "The seed has sprouted and is establishing roots."
                    percent < 50 -> "Vegetative" to "The plant is growing leaves and stems rapidly."
                    percent < 80 -> "Flowering/Fruiting" to "Flowers or fruit are beginning to form."
                    percent < 100 -> "Maturation" to "The crop is ripening and nearing harvest."
                    else -> "Ready to Harvest" to "Fully grown and ready for harvest."
                }

                view?.setGrowthDetails(percent, stage, desc)
                val isReadyToHarvest = (percent >= 100 || today >= minHarvest) && (crop.harvestedDate == null)
                view?.showHarvestButton(isReadyToHarvest)
            }
            view?.showMapButton(crop.latitude != 0.0)
        }
    }

    fun onViewOnMapClicked(viewModel: CropViewModel) {
        val currentCrop = viewModel.cropData.value
        if (currentCrop != null && currentCrop.latitude != 0.0) {
            viewModel.cropToFocus = currentCrop
            viewModel.isMapPickerMode = false
            view?.navigateToMap(currentCrop)
        }
    }

    fun onHarvestClicked(viewModel: CropViewModel) {
        val currentCrop = viewModel.cropData.value ?: return
        view?.showHarvestConfirmation("Harvest ${currentCrop.cropName} now?") {
            viewModel.harvestCrop(currentCrop, System.currentTimeMillis())
            view?.navigateToGarden()
        }
    }

    fun onDestroy() {
        view = null
    }
}