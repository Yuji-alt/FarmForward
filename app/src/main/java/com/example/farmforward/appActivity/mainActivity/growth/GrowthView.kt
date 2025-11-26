package com.example.farmforward.appActivity.mainActivity.growth

import android.content.Context

interface GrowthView {
    fun getFragmentContext(): Context

    fun setCropName(name: String)
    fun setArea(area: String)
    fun setPlantedDate(date: String)
    fun setMinHarvest(date: String)
    fun setMaxHarvest(date: String)
    fun setYield(yield: String)
    fun setSoil(soil: String)
    fun setIrrigation(irrigation: String)
    fun setDensity(density: String)
    fun setFertilizer(fertilizer: String)
    fun setCropImage(resourceId: Int)
    fun setWeather(weather: String)
    fun showEmptyState()
}