package com.example.farmforward.appActivity.mainActivity.growth

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.farmforward.database.CropEntity

interface GrowthView {
    fun getFragmentContext(): Context
    fun getScope(): LifecycleCoroutineScope

    fun displayCrops(crops: List<CropEntity>)
    fun navigateToCropDetails(crop: CropEntity)
}