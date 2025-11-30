package com.example.farmforward.appActivity.mainActivity.calc

import android.widget.Toast
import com.example.farmforward.database.CropEntity

interface CalcView {
    fun showToast(message: String, isError: Boolean = false)

    fun setCropAdapter(cropNames: List<String>)
    fun setFactorAdapters(
        soilOptions: List<String>,
        irrigationOptions: List<String>,
        densityOptions: List<String>,
        fertOptions: List<String>
    )
    fun clearFactorInputs()
    fun clearAllInputs()

    fun navigateToLoading(isOnline: Boolean)
    fun getCurrentLocation(onLocationFound: (Double, Double) -> Unit)
    fun preFillForm(crop: CropEntity)
    fun setButtonText(text: String)
}