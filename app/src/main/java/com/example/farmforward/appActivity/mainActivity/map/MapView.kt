package com.example.farmforward.appActivity.mainActivity.map

import android.content.Context
import com.example.farmforward.database.CropEntity

interface MapView {
    fun getFragmentContext(): Context
    fun displayCropsOnMap(crops: List<CropEntity>)
    fun navigateToCropDetails()
    fun showToast(message: String)
}