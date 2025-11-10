package com.example.farmforward.fragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.activityController.MainController
import com.example.farmforward.activityViewmodel.MainActivity
import com.example.farmforward.fragmentController.HomeController
import com.example.farmforward.fragmentController.WeatherController
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.session.SessionManager
import com.example.farmforward.utils.RetrofitClient
import com.example.farmforward.utils.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.location.Geocoder
import androidx.core.content.ContentProviderCompat.requireContext
import java.util.Locale



class HomeFragment : Fragment() {

    private lateinit var controller: HomeController
    private lateinit var searchInput: EditText
    private lateinit var itemContainer: LinearLayout
    private lateinit var menuButton: ImageButton
    private var refreshJob: Job? = null

    private var userId: Int? = null
    private lateinit var weatherController: WeatherController
    private lateinit var weatherContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var weatherText: TextView


    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        weatherContainer = view.findViewById(R.id.weatherContainer)
        weatherController = WeatherController(requireContext(), weatherContainer)
        searchInput = view.findViewById(R.id.search_input)
        itemContainer = view.findViewById(R.id.itemContainer)
        menuButton = view.findViewById(R.id.menu_button)
        locationText = view.findViewById(R.id.locationText)
        weatherText = view.findViewById(R.id.weather_date)
        controller = HomeController(requireContext(), itemContainer)

        val activityContext = activity as? MainActivity ?: return view
        val mainController = activityContext.controller

        val session = SessionManager(requireContext())
        userId = session.getUserId()

        menuButton.setOnClickListener {
            activityContext.openDrawer()
        }
        fetchWeatherByLocation(mainController, activityContext)

        refreshData()
        return view
    }

    private fun fetchWeatherByLocation(mainController: MainController, activity: MainActivity) {
        if (!mainController.hasLocationPermission()) {
            mainController.requestLocationPermission(activity)
            Toast.makeText(requireContext(), "Please grant location permission", Toast.LENGTH_SHORT).show()
            return
        }

        mainController.fetchCurrentLocation(activity) { lat, lon ->
            val locationName = getLocationName(lat, lon)
            locationText.text = locationName
            fetchWeatherForecast(lat, lon)
        }
    }

    private fun fetchWeatherForecast(lat: Double, lon: Double) {
        val apiKey = BuildConfig.WEATHER_API_KEY

        RetrofitClient.instance.getForecastByCoordinates(lat, lon, apiKey)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                    if (response.isSuccessful) {
                        val allForecasts = response.body()?.list ?: return
                        val todayDateString = getWeatherDay()
                        weatherText.text = todayDateString
                        val todayForecasts = allForecasts.filter { it.dt_txt.startsWith(getTodayDate()) }
                        weatherController.displayForecast(todayForecasts)
                    } else {
                        Log.d("WeatherAPI", "API Error: ${response.code()} ${response.message()}")
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    Log.d("WeatherAPI", "Network Error: ${t.localizedMessage}")
                }
            })
    }
    private fun getLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                address.locality ?: address.subAdminArea ?: "Unknown location"
            } else {
                "Unknown location"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown location"
        }
    }


    private fun getTodayDate(): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return format.format(java.util.Date())

    }private fun getWeatherDay(): String {
        val format = java.text.SimpleDateFormat("EEEE, MMMM dd", java.util.Locale.getDefault())
        return format.format(java.util.Date())
    }


    override fun onResume() {
        super.onResume()
        val activityContext = activity as? MainActivity ?: return
        fetchWeatherByLocation(activityContext.controller, activityContext)
    }

    fun refreshData() {
        refreshJob?.cancel()
        val id = userId ?: return
        val db = AppDatabase.getDatabase(requireContext())

        refreshJob = lifecycleScope.launch(Dispatchers.IO) {
            val crops = db.cropDao().getCropsForUserList(id)
            withContext(Dispatchers.Main) {
                controller.displayCrops(crops)
            }
        }
    }
}