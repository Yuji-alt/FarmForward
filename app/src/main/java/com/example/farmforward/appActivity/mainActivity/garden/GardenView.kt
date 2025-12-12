package com.example.farmforward.appActivity.mainActivity.garden

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.farmforward.database.CropEntity

interface GardenView {
    fun getFragmentContext(): Context
    fun getScope(): LifecycleCoroutineScope
    fun navigateToGrowth()
    fun navigateToCalc()
    fun selectCropForGrowth(crop: CropEntity)
    fun updateCropLists(
        growingCrops: List<CropEntity>,
        readyCrops: List<CropEntity>,
        harvestedCrops: List<CropEntity>
    )
}