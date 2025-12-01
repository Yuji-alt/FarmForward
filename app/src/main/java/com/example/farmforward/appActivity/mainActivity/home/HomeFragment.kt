package com.example.farmforward.appActivity.mainActivity.home

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.CropEntity
import com.example.farmforward.utils.weatherUtils.ForecastItem
import com.example.farmforward.utils.weatherUtils.WeatherController
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.CropImageHelper
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), HomeView {

    @Inject lateinit var controller: HomeController
    private lateinit var weatherController: WeatherController

    private lateinit var searchInput: EditText
    private lateinit var appLogo: ImageView
    private lateinit var searchButton: ImageButton
    private lateinit var itemContainer: LinearLayout
    private lateinit var menuButton: ImageButton
    private lateinit var weatherContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var weatherText: TextView
    private lateinit var cropViewModel: CropViewModel
    private lateinit var activeCropContainer: LinearLayout
    private var isSearchOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        searchInput = view.findViewById(R.id.search_input)
        appLogo = view.findViewById(R.id.app_logo)
        searchButton = view.findViewById(R.id.search_button)
        weatherContainer = view.findViewById(R.id.weatherContainer)
        searchInput = view.findViewById(R.id.search_input)
        itemContainer = view.findViewById(R.id.cropItemContainer)
        menuButton = view.findViewById(R.id.menu_button)
        locationText = view.findViewById(R.id.locationText)
        weatherText = view.findViewById(R.id.weather_date)
        activeCropContainer = view.findViewById(R.id.active_crop_container)
        weatherController = WeatherController(requireContext(), weatherContainer)

        controller.bindView(this, viewLifecycleOwner.lifecycleScope)
        controller.setupObserver(viewLifecycleOwner)

        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        setupSearchLogic()
    }

    override fun displayCrops(crops: List<CropEntity>) {
        itemContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        val addView = inflater.inflate(R.layout.item_add_crop, itemContainer, false)
        addView.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)
        }

        // 1. Display Existing Crops
        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.item_crop_card, itemContainer, false)

            val itemTitle = itemView.findViewById<TextView>(R.id.itemTitle)
            val imgCrop = itemView.findViewById<ImageView>(R.id.itemImage)
            imgCrop.setImageResource(CropImageHelper.getImageRes(crop.cropName))
            imgCrop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.moss_green))
            itemTitle.text = crop.cropName

            itemView.setOnClickListener {
                cropViewModel.viewCropDetails(crop)
                cropViewModel.lastSourceId = R.id.nav_home

                (activity as? MainActivity)?.controller?.onNavigationItemClicked(MainActivity.NAV_CROP_DETAILS)
            }

            itemContainer.addView(itemView)
        }
        itemContainer.addView(addView)
    }

    override fun displayActiveStatus(crops: List<CropEntity>) {
        activeCropContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (crop in crops) {
            val view = inflater.inflate(R.layout.active_status, activeCropContainer, false)

            val tvCropName = view.findViewById<TextView>(R.id.tvCropName)
            val tvDays = view.findViewById<TextView>(R.id.tvDays)
            val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
            val imgCrop = view.findViewById<ImageView>(R.id.imgCrop)

            imgCrop.setImageResource(CropImageHelper.getImageRes(crop.cropName))
            imgCrop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.moss_green))

            tvCropName.text = crop.cropName

            val status = calculateStatus(crop)

            tvDays.text = status.daysText
            tvStatus.text = status.statusText


            view.setOnClickListener {
                cropViewModel.viewCropDetails(crop)
                cropViewModel.lastSourceId = R.id.nav_home
                (activity as? MainActivity)?.controller?.onNavigationItemClicked(MainActivity.NAV_CROP_DETAILS)
            }

            activeCropContainer.addView(view)
        }
        val addView = inflater.inflate(R.layout.add_crop, activeCropContainer, false)
        addView.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)
        }

        activeCropContainer.addView(addView)
    }

    private fun setupSearchLogic() {
        searchButton.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else {
                openSearch()
            }
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                controller.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun openSearch() {
        isSearchOpen = true
        appLogo.visibility = View.GONE
        searchInput.visibility = View.VISIBLE
        searchButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        searchInput.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        isSearchOpen = false
        searchInput.setText("")
        appLogo.visibility = View.VISIBLE
        searchInput.visibility = View.GONE
        searchButton.setImageResource(android.R.drawable.ic_menu_search)
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    data class CropStatus(val daysText: String, val statusText: String, val colorRes: Int)

    private fun calculateStatus(crop: CropEntity): CropStatus {
        val today = System.currentTimeMillis()
        if (today < crop.date) {
            val diff = crop.date - today
            val days = TimeUnit.MILLISECONDS.toDays(diff) + 1
            return CropStatus("$days DAYS", "SCHEDULED", android.R.color.holo_blue_dark)
        }

        val minHarvest = crop.mindate ?: return CropStatus("---", "GROWING", R.color.kombuGreen)
        val maxHarvest = crop.maxdate ?: 0L
        if (maxHarvest != 0L && today > maxHarvest) {
            val diff = today - maxHarvest
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            return CropStatus("$days DAYS", "OVERDUE", android.R.color.holo_red_dark)
        }

        val diff = minHarvest - today
        val daysDiff = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            daysDiff <= 0L -> CropStatus("NOW", "HARVEST", R.color.kombuGreen)
            daysDiff < 7 -> CropStatus("$daysDiff DAYS", "SOON", android.R.color.holo_orange_dark)
            else -> CropStatus("$daysDiff DAYS", "GROWING", R.color.kombuGreen)
        }
    }

    override fun onResume() {
        super.onResume()
        controller.onViewResumed()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.onDestroy()
    }

    fun onPermissionGranted() {
        controller.onPermissionGranted()
    }

    fun onPermissionDenied() {
        controller.onPermissionDenied()
    }

    override fun setLocationText(text: String) {
        if (view != null) locationText.text = text
    }

    override fun setWeatherDateText(text: String) {
        if (view != null) weatherText.text = text
    }

    override fun displayForecast(forecasts: List<ForecastItem>) {
        if (view != null) weatherController.displayForecast(forecasts)
    }

    override fun showWeatherContainer(isVisible: Boolean) {
        if (view != null) {
            weatherContainer.visibility = if (isVisible) View.VISIBLE else View.GONE
        }
    }

    override fun showToast(message: String, isError: Boolean) {
        (activity as? MainActivity)?.showToast(message, isError)
    }

    override fun getMainActivity(): MainActivity? {
        return activity as? MainActivity
    }
}