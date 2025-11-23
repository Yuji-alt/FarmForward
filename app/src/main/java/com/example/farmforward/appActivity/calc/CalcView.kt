package com.example.farmforward.appActivity.calc

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope

interface CalcView {
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)

    fun setCropAdapter(cropNames: List<String>)
    fun setFactorAdapters(
        soilOptions: List<String>,
        irrigationOptions: List<String>,
        densityOptions: List<String>,
        fertOptions: List<String>
    )
    fun clearFactorInputs()
    fun clearAllInputs()
    fun navigateToLoading()

}