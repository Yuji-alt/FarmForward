package com.example.farmforward.appActivity.mainActivity.calc

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
    fun onCalculationSuccess()
    fun clearAllInputs()

    fun navigateToLoading(isOnline: Boolean)
    fun updateLoading(progress: Int, message: String)

    fun getCurrentLocation(onLocationFound: (Double, Double) -> Unit)
    fun preFillForm(crop: CropEntity)
    fun setButtonText(text: String)
}