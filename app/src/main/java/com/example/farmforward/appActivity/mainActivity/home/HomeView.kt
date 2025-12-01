package com.example.farmforward.appActivity.mainActivity.home
import com.example.farmforward.database.CropEntity
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.utils.weatherUtils.ForecastItem

interface HomeView {
    fun displayCrops(crops: List<CropEntity>)
    fun displayActiveStatus(crops: List<CropEntity>)
    fun setLocationText(text: String)
    fun setWeatherDateText(text: String)
    fun displayForecast(forecasts: List<ForecastItem>)
    fun showWeatherContainer(isVisible: Boolean)
    fun showToast(message: String, isError: Boolean = false)
    fun getMainActivity(): MainActivity?

}
