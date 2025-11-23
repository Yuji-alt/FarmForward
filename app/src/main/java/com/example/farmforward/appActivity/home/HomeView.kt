package com.example.farmforward.appActivity.home
import com.example.farmforward.database.roomDatabase.CropEntity
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.utils.ForecastItem

interface HomeView {
    fun displayCrops(crops: List<CropEntity>)
    fun setLocationText(text: String)
    fun setWeatherDateText(text: String)
    fun displayForecast(forecasts: List<ForecastItem>)
    fun showWeatherContainer(isVisible: Boolean)
    fun showToast(message: String, duration: Int)
    fun getMainActivity(): MainActivity?
}
