package com.example.farmforward.appActivity.mainActivity.otherFragment.GardenTools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.utils.CropImageHelper
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class GardenToolsFragment : Fragment() {

    @Inject lateinit var db: AppDatabase
    @Inject lateinit var session: SessionManager
    @Inject lateinit var weatherRepository: WeatherRepository

    private var mode: String = "HARVEST"
    private lateinit var cropListContainer: LinearLayout // Renamed for clarity
    private lateinit var titleLabel: TextView

    companion object {
        fun newInstance(mode: String): GardenToolsFragment {
            val fragment = GardenToolsFragment()
            val args = Bundle()
            args.putString("MODE", mode)
            fragment.arguments = args
            return fragment
        }
    }

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
        titleLabel = view.findViewById(R.id.tvAddLabel)
        val btnBack = view.findViewById<ImageButton>(R.id.btn_close_nav)

        btnBack.setOnClickListener {
            // Using the new Profile back logic or just default home
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_home)
        }

        if (mode == "HARVEST") {
            titleLabel.text = "Harvest History"
            loadHarvestHistory(inflater)
        } else {
            titleLabel.text = "Weather Forecast"
            loadWeather(inflater)
        }

        return view
    }

    private fun loadHarvestHistory(inflater: LayoutInflater) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            val harvestedCrops = db.cropDao().getHarvestedCrops(userId)

            withContext(Dispatchers.Main) {
                cropListContainer.removeAllViews()

                if (harvestedCrops.isEmpty()) {
                    showEmptyState("No harvested crops yet.")
                }

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

    private fun loadWeather(inflater: LayoutInflater) {
        weatherRepository.loadCachedData()
        val forecasts = weatherRepository.cachedForecasts

        if (forecasts.isNullOrEmpty()) {
            showEmptyState("Weather data not available.\nPlease sync at Home.")
            return
        }

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

    private fun addToContainer(view: View) {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(16, 8, 16, 8)
        view.layoutParams = params
        cropListContainer.addView(view)
    }

    private fun showEmptyState(message: String) {
        val emptyView = TextView(context)
        emptyView.text = message
        emptyView.textSize = 18f
        emptyView.setPadding(30, 50, 30, 50)
        cropListContainer.addView(emptyView)
    }
}