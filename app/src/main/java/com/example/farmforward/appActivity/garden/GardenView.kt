package com.example.farmforward.appActivity.garden

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.farmforward.database.roomDatabase.CropEntity

interface GardenView {
    fun getFragmentContext(): Context
    fun getScope(): LifecycleCoroutineScope

    fun displayCrops(crops: List<CropEntity>)
    fun navigateToGrowth()
    fun navigateToCalc()

    fun getCurrentUserId(): Int?
    fun selectCropForGrowth(crop: CropEntity)
}