package com.example.farmforward.appActivity.mainActivity.otherFragment.GrowthCropDetails

import android.content.Context
import com.example.farmforward.database.CropEntity

interface GrowthCropDetailsView {
    fun getFragmentContext(): Context
    fun setCropName(name: String)
    fun setCropImage(resourceId: Int)
    fun setCropImageTint(colorRes: Int)
    fun setGrowthDetails(percent: Int, stage: String, description: String)
    fun setPlantedDate(date: String)
    fun setEstimatedHarvest(date: String)

    fun showEmptyState()
    fun showHarvestButton(isVisible: Boolean)
    fun showMapButton(isVisible: Boolean)

    fun navigateBack(destinationId: Int)
    fun navigateToMap(crop: CropEntity)
    fun navigateToGrowth()
    fun showHarvestConfirmation(message: String, onConfirm: () -> Unit)
}
