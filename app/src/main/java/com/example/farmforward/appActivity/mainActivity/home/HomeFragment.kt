package com.example.farmforward.appActivity.mainActivity.home

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.roomDatabase.CropEntity
import com.example.farmforward.utils.weatherUtils.ForecastItem
import com.example.farmforward.utils.weatherUtils.WeatherController
import com.example.farmforward.database.viewModel.CropViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), HomeView {

    @Inject lateinit var controller: HomeFragmentController
    private lateinit var weatherController: WeatherController

    private lateinit var searchInput: EditText
    private lateinit var itemContainer: LinearLayout
    private lateinit var menuButton: ImageButton
    private lateinit var weatherContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var weatherText: TextView
    private lateinit var cropViewModel: CropViewModel

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

        weatherContainer = view.findViewById(R.id.weatherContainer)
        searchInput = view.findViewById(R.id.search_input)
        itemContainer = view.findViewById(R.id.itemContainer)
        menuButton = view.findViewById(R.id.menu_button)
        locationText = view.findViewById(R.id.locationText)
        weatherText = view.findViewById(R.id.weather_date)

        weatherController = WeatherController(requireContext(), weatherContainer)

        controller.bindView(this, viewLifecycleOwner.lifecycleScope)
        controller.setupObserver(viewLifecycleOwner)

        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        setupSearchListener()
    }
    override fun displayCrops(crops: List<CropEntity>) {
        itemContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        val addView = inflater.inflate(R.layout.item_add_crop, itemContainer, false)
        addView.findViewById<View>(R.id.itemImage).setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)
        }
        itemContainer.addView(addView)

        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.item_crop_card, itemContainer, false)

            val itemTitle = itemView.findViewById<TextView>(R.id.itemTitle)
            val itemDesc = itemView.findViewById<TextView>(R.id.itemDesc)
            val itemArea = itemView.findViewById<TextView>(R.id.itemArea)

            itemTitle.text = crop.cropName
            itemDesc.text = "Expected Yield: ${crop.expectedYield} kg"
            itemArea.text = "Area: ${crop.area} sqr. meter"

            itemView.setOnClickListener {
                cropViewModel.viewCropDetails(crop)
                (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_growth)
            }

            itemContainer.addView(itemView)
        }
    }
    private fun setupSearchListener() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                controller.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
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