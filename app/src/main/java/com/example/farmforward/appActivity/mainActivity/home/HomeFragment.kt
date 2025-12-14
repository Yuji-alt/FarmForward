package com.example.farmforward.appActivity.mainActivity.home

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
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
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.CropImageHelper
import com.example.farmforward.utils.weatherUtils.ForecastItem
import com.example.farmforward.utils.weatherUtils.WeatherController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment(), HomeView {

    // -------------------------------------------------------------------------
    // Dependencies & Variables
    // -------------------------------------------------------------------------
    @Inject lateinit var controller: HomeController
    private lateinit var weatherController: WeatherController
    private lateinit var cropViewModel: CropViewModel

    // UI Elements
    private lateinit var searchInput: EditText
    private lateinit var appLogo: ImageView
    private lateinit var searchButton: ImageButton
    private lateinit var itemContainer: LinearLayout
    private lateinit var menuButton: ImageButton
    private lateinit var weatherContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var weatherText: TextView

    private var isSearchOpen = false

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent?.action) {
                // GPS state changed! Force a refresh.
                controller.onViewResumed()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle Methods
    // -------------------------------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init ViewModel
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        // Init Views
        initViews(view)

        // Init Controllers
        weatherController = WeatherController(requireContext(), weatherContainer)
        controller.bindView(this, viewLifecycleOwner.lifecycleScope)
        controller.setupObserver(viewLifecycleOwner)

        // Setup Listeners
        menuButton.setOnClickListener { (activity as? MainActivity)?.openDrawer() }
        setupSearchLogic()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        requireContext().registerReceiver(gpsReceiver, filter)
        controller.onViewResumed()
    }
    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(gpsReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        controller.onDestroy()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            controller.onViewResumed()
        }
    }

    private fun initViews(view: View) {
        searchInput = view.findViewById(R.id.search_input)
        appLogo = view.findViewById(R.id.app_logo)
        searchButton = view.findViewById(R.id.search_button)
        weatherContainer = view.findViewById(R.id.weatherContainer)
        itemContainer = view.findViewById(R.id.cropItemContainer)
        menuButton = view.findViewById(R.id.menu_button)
        locationText = view.findViewById(R.id.locationText)
        weatherText = view.findViewById(R.id.weather_date)
    }

    // -------------------------------------------------------------------------
    // View Interface Implementation (UI Updates)
    // -------------------------------------------------------------------------
    override fun displayCrops(crops: List<CropEntity>) {
        itemContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        // 1. Add Crop Items
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

        // 2. Add "Add New Crop" Button first
        val addView = inflater.inflate(R.layout.item_add_crop, itemContainer, false)
        val navToCalc = View.OnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)
        }
        addView.setOnClickListener(navToCalc)
        addView.findViewById<View>(R.id.itemImage)?.setOnClickListener(navToCalc)
        itemContainer.addView(addView)
    }

    override fun displayActiveStatus(crops: List<CropEntity>) {
        // Logic removed as per request to remove Active Crops section
    }

    // -------------------------------------------------------------------------
    // Search Logic
    // -------------------------------------------------------------------------
    private fun setupSearchLogic() {
        searchButton.setOnClickListener {
            if (isSearchOpen) closeSearch() else openSearch()
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
    // -------------------------------------------------------------------------
    // Helpers & Permissions
    // -------------------------------------------------------------------------
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

    override fun getMainActivity(): MainActivity? = activity as? MainActivity

    fun onPermissionGranted() = controller.onPermissionGranted()
    fun onPermissionDenied() = controller.onPermissionDenied()
}