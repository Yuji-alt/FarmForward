package com.example.farmforward.appActivity.home

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
import com.example.farmforward.utils.ForecastItem
import com.example.farmforward.utils.WeatherController
import com.example.farmforward.viewModel.CropViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint // REQUIRED: Enables Hilt injection for this Fragment
class HomeFragment : Fragment(), HomeView {

    // HILT INJECTION: Controller is automatically created and injected
    @Inject lateinit var controller: HomeFragmentController

    // UI Helpers (Manually created as they are View Adapters)
    private lateinit var cropsDisplayController: HomeController
    private lateinit var weatherController: WeatherController

    // UI Elements
    private lateinit var searchInput: EditText
    private lateinit var itemContainer: LinearLayout
    private lateinit var menuButton: ImageButton
    private lateinit var weatherContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var weatherText: TextView

    // ViewModel
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

        cropsDisplayController = HomeController(requireContext(), itemContainer)
        weatherController = WeatherController(requireContext(), weatherContainer)

        controller.bindView(this, viewLifecycleOwner.lifecycleScope)

        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        setupSearchListener()

        controller.setupObserver(viewLifecycleOwner)
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

    override fun displayCrops(crops: List<CropEntity>) {
        cropsDisplayController.displayCrops(crops) { selectedCrop ->
            cropViewModel.viewCropDetails(selectedCrop)
            (activity as? MainActivity)?.controller?.switchFragment(R.id.nav_growth)
        }
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

    override fun showToast(message: String, duration: Int) {
        context?.let {
            Toast.makeText(it, message, duration).show()
        }
    }

    override fun getMainActivity(): MainActivity? {
        return activity as? MainActivity
    }
}