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
import com.example.farmforward.utils.LoadingDialog
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
    private var lastWeatherFetchTime: Long = 0L
    private val WEATHER_FETCH_COOLDOWN = 10 * 60 * 1000L
    private lateinit var loadingDialog: LoadingDialog

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
        val session = SessionManager(requireContext())
        userId = session.getUserId()
        loadingDialog = LoadingDialog(requireContext())

        menuButton.setOnClickListener {
            activityContext.openDrawer()
        }

        refreshData()
        return view
    }

    private fun fetchWeatherByLocation(mainController: MainController, activity: MainActivity) {

        loadingDialog.show()
        weatherContainer.visibility = View.GONE
        locationText.text = getString(R.string.location_loading)
        weatherText.text = getString(R.string.loading)

        mainController.checkAndRequestLocationPermission(
            activity,
            onPermissionGranted = {
                mainController.fetchCurrentLocation(activity) { lat, lon ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val locationName = getLocationName(lat, lon)
                        withContext(Dispatchers.Main) {
                            locationText.text = locationName
                        }
                        fetchWeatherForecast(lat, lon)
                    }
                }
            },
            onPermissionDenied = {
                onPermissionDenied()
            }
        )
    }
    fun onPermissionGranted() {
        Log.d("HomeFragment", "Permission was granted! Re-fetching weather.")
        // Re-run the fetch logic
        val activityContext = activity as? MainActivity ?: return
        fetchWeatherByLocation(activityContext.controller, activityContext)
    }

    fun onPermissionDenied() {
        Log.d("HomeFragment", "Permission was denied.")
        loadingDialog.dismiss()
        locationText.text = getString(R.string.permission_needed)
        weatherText.text = "---"
    }

    private fun fetchWeatherForecast(lat: Double, lon: Double) {
        val apiKey = BuildConfig.WEATHER_API_KEY

        RetrofitClient.instance.getForecastByCoordinates(lat, lon, apiKey)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {

                    loadingDialog.dismiss()
                    weatherContainer.visibility = View.VISIBLE

                    if (response.isSuccessful) {
                        val allForecasts = response.body()?.list ?: return
                        val todayDateString = getWeatherDay()
                        weatherText.text = todayDateString
                        val todayForecasts = allForecasts.filter { it.dt_txt.startsWith(getTodayDate()) }
                        weatherController.displayForecast(todayForecasts)
                    } else {
                        Log.d("WeatherAPI", "API Error: ${response.code()} ${response.message()}")
                        val (userMessage, errorTitle) = when (response.code()) {
                            401 -> "API key is invalid." to "API Error"
                            429 -> "Rate limit exceeded. Please try again later." to "Error"
                            500 -> "Server error. Please try again later." to "Error"
                            else -> "Failed to load weather." to "Error"
                        }
                        Toast.makeText(requireContext(), userMessage, Toast.LENGTH_LONG).show()
                        weatherText.text = errorTitle
                        locationText.text = "---"
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    loadingDialog.dismiss()
                    weatherContainer.visibility = View.VISIBLE
                    Log.e("WeatherAPI", "Network Error: ${t.localizedMessage}")
                    Toast.makeText(requireContext(), "Network error. Check connection.", Toast.LENGTH_SHORT).show()
                    weatherText.text = getString(R.string.network_failed)
                    locationText.text = " "
                }
            })
    }
    private fun getLocationName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                // Using adminArea as requested
                address.adminArea ?: address.locality ?: "Unknown Location"
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown Location"
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
        val now = System.currentTimeMillis()
        if (now - lastWeatherFetchTime > WEATHER_FETCH_COOLDOWN) {
            lastWeatherFetchTime = now
            fetchWeatherByLocation(activityContext.controller, activityContext)

        } else {
            Log.d("HomeFragment", "Skipping weather fetch, not enough time passed.")
        }
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