package com.example.farmforward.appActivity.mainActivity.otherFragment.GardenTools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.utils.CropImageHelper
import com.example.farmforward.utils.weatherUtils.ForecastItem
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class GardenToolsFragment : Fragment() {

    // ---------------------------------------------------------------------------------------------
    // Dependencies & Variables
    // ---------------------------------------------------------------------------------------------
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var session: SessionManager
    @Inject lateinit var weatherRepository: WeatherRepository

    private var mode: String = "HARVEST"
    private lateinit var cropListContainer: LinearLayout
    private lateinit var graphContainer: LinearLayout
    private lateinit var locationDisplayContainer: LinearLayout // NEW Container
    private lateinit var tvLocationName: TextView // NEW TextView
    private lateinit var temperatureChart: LineChart
    private lateinit var titleLabel: TextView
    private var tvEmptyState: TextView? = null

    companion object {
        fun newInstance(mode: String): GardenToolsFragment {
            val fragment = GardenToolsFragment()
            val args = Bundle()
            args.putString("MODE", mode)
            fragment.arguments = args
            return fragment
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Methods
    // ---------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mode = arguments?.getString("MODE") ?: "HARVEST"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.garden_tools, container, false)

        cropListContainer = view.findViewById(R.id.cropListContainer)
        graphContainer = view.findViewById(R.id.graphContainer)
        temperatureChart = view.findViewById(R.id.temperatureChart)
        titleLabel = view.findViewById(R.id.tvAddLabel)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        locationDisplayContainer = view.findViewById(R.id.locationDisplayContainer)
        tvLocationName = view.findViewById(R.id.tvLocationName)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_close_nav)

        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_home)
        }

        if (mode == "HARVEST") {
            titleLabel.text = "Harvest History"
            graphContainer.visibility = View.GONE
            locationDisplayContainer.visibility = View.GONE
            loadHarvestHistory(inflater)
        } else {
            titleLabel.text = "Weather Forecast"
            locationDisplayContainer.visibility = View.VISIBLE
            loadWeather(inflater)
        }

        return view
    }

    // ---------------------------------------------------------------------------------------------
    // Data Loading Logic
    // ---------------------------------------------------------------------------------------------
    private fun loadHarvestHistory(inflater: LayoutInflater) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            val harvestedCrops = db.cropDao().getHarvestedCrops(userId)

            withContext(Dispatchers.Main) {
                cropListContainer.removeAllViews()

                if (harvestedCrops.isEmpty()) {
                    toggleEmptyState(true, "No harvested crops yet.")
                } else {
                    toggleEmptyState(false)
                    for (crop in harvestedCrops) {
                        val itemView = inflater.inflate(R.layout.garden_frame, cropListContainer, false)

                        val tvName = itemView.findViewById<TextView>(R.id.tvCropName)
                        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
                        val img = itemView.findViewById<ImageView>(R.id.imgCrop)

                        tvName.text = crop.cropName
                        val date = Date(crop.harvestedDate ?: System.currentTimeMillis())
                        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        tvStatus.text = "Harvested: ${format.format(date)}"
                        img.setImageResource(CropImageHelper.getImageRes(crop.cropName))
                        val color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.moss_green)
                        img.setColorFilter(color)

                        addToContainer(itemView)
                    }
                }
            }
        }
    }

    private fun loadWeather(inflater: LayoutInflater) {
        weatherRepository.loadCachedData()
        val forecasts = weatherRepository.cachedForecasts

        // NEW: Set Location Text from cached data
        tvLocationName.text = weatherRepository.cachedLocationName ?: "Unknown Location"

        if (forecasts.isNullOrEmpty()) {
            toggleEmptyState(true, "Weather data not available.\nPlease sync at Home.")
            cropListContainer.removeAllViews()
            graphContainer.visibility = View.GONE
            return
        }

        // Show Graph Container and Populate Graph
        graphContainer.visibility = View.VISIBLE
        displayWeatherGraph(forecasts)

        // Show List
        toggleEmptyState(false)
        cropListContainer.removeAllViews()
        val dateFormat = SimpleDateFormat("EEE, MMM dd h:mm a", Locale.getDefault())

        for (item in forecasts) {
            val itemView = inflater.inflate(R.layout.garden_frame, cropListContainer, false)

            val tvName = itemView.findViewById<TextView>(R.id.tvCropName)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val img = itemView.findViewById<ImageView>(R.id.imgCrop)
            val date = Date(item.dt * 1000)
            tvName.text = dateFormat.format(date)

            val weatherDesc = item.weather.firstOrNull()?.description ?: ""
            val temp = item.main.temp
            tvStatus.text = "$weatherDesc - ${temp}°C"
            val mainWeather = item.weather.firstOrNull()?.main ?: ""

            val iconRes: Int
            val iconColorRes: Int

            when {
                mainWeather.contains("Rain", ignoreCase = true) ||
                        mainWeather.contains("Drizzle", ignoreCase = true) -> {
                    iconRes = R.drawable.rain
                    iconColorRes = R.color.cafenoir
                }
                mainWeather.contains("Thunder", ignoreCase = true) -> {
                    iconRes = R.drawable.thunder_storm
                    iconColorRes = R.color.cafenoir
                }
                mainWeather.contains("Clear", ignoreCase = true) -> {
                    iconRes = R.drawable.clear_sky
                    iconColorRes = R.color.cafenoir
                }
                else -> {
                    iconRes = R.drawable.broken_clouds
                    iconColorRes = R.color.cafenoir
                }
            }

            img.setImageResource(iconRes)
            img.background = null
            val iconColor = androidx.core.content.ContextCompat.getColor(requireContext(), iconColorRes)
            img.setColorFilter(iconColor)

            addToContainer(itemView)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // MPAndroidChart GRAPH
    // ---------------------------------------------------------------------------------------------
    private fun displayWeatherGraph(forecasts: List<ForecastItem>) {

        val entries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        val timeFormat = SimpleDateFormat("h a", Locale.getDefault())

        // 1. Convert Forecast data into Chart Entries
        forecasts.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.main.temp.toFloat()))
            labels.add(timeFormat.format(Date(item.dt * 1000L)))
        }

        // 2. Setup Dataset (Line Style)
        val dataSet = LineDataSet(entries, "Temperature (°C)").apply {
            val primaryColor = ContextCompat.getColor(requireContext(), R.color.kombuGreen)

            color = primaryColor
            valueTextColor = primaryColor
            lineWidth = 2.5f
            circleRadius = 4f
            setCircleColor(primaryColor)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = primaryColor
            fillAlpha = 50
            valueTextSize = 10f
        }

        // 3. Apply Data to Chart
        val lineData = LineData(dataSet)
        temperatureChart.data = lineData

        // 4. Customize X-Axis (Time Labels)
        temperatureChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textColor = ContextCompat.getColor(requireContext(), R.color.kombuGreen)
            setSpaceMin(0.5f)
            setSpaceMax(0.5f)
        }

        // 5. Customize Y-Axis (Temperature)
        temperatureChart.axisLeft.apply {
            setDrawGridLines(true)
            textColor = ContextCompat.getColor(requireContext(), R.color.kombuGreen)
            val minTemp = entries.minOfOrNull { it.y }?.minus(1f) ?: 0f
            val maxTemp = entries.maxOfOrNull { it.y }?.plus(1f) ?: 50f
            axisMinimum = minTemp
            axisMaximum = maxTemp
        }
        temperatureChart.axisRight.isEnabled = false

        // 6. General Chart Settings
        temperatureChart.description.isEnabled = false
        temperatureChart.legend.isEnabled = false
        temperatureChart.setTouchEnabled(true)
        temperatureChart.animateX(800)
        temperatureChart.invalidate()
    }


    // ---------------------------------------------------------------------------------------------
    // UI Helper Methods
    // ---------------------------------------------------------------------------------------------
    private fun addToContainer(view: View) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 8, 16, 8)
        view.layoutParams = params
        cropListContainer.addView(view)
    }

    private fun toggleEmptyState(isEmpty: Boolean, message: String = "") {
        if (isEmpty) {
            tvEmptyState?.text = message
            tvEmptyState?.visibility = View.VISIBLE
            cropListContainer.visibility = View.GONE
        } else {
            tvEmptyState?.visibility = View.GONE
            cropListContainer.visibility = View.VISIBLE
        }
    }
}