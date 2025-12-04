package com.example.farmforward.appActivity.mainActivity.otherFragment.CropDetails

import android.content.Context
import com.example.farmforward.database.CropEntity

interface CropDetailsView {
    fun getFragmentContext(): Context
    fun setCropName(name: String)
    fun setArea(area: String)
    fun setCropImageTint(colorRes: Int)
    fun setPlantedDate(date: String)
    fun setYield(yield: String)
    fun setSoil(soil: String)
    fun setIrrigation(irrigation: String)
    fun setDensity(density: String)
    fun setFertilizer(fertilizer: String)
    fun setCropImage(resourceId: Int)
    fun setWeather(weather: String)

    fun showEmptyState()
    fun navigateToEdit(crop: CropEntity)
    fun showDeleteConfirmation(message: String, onConfirm: () -> Unit)
    fun navigateToGarden()
    fun navigateToMap(crop: CropEntity)
    fun navigateBack(destinationId: Int)
    fun showMapButton(isVisible: Boolean)
    fun showHarvestButton(isVisible: Boolean)
    fun showHarvestDatePicker(minHarvestDate: Long, onDateSelected: (Long) -> Unit)
    fun setLocation(region: String, locality: String)}