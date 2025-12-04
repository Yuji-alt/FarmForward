package com.example.farmforward.appActivity.mainActivity.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : Fragment(), MapView, OnMapReadyCallback {

    // -------------------------------------------------------------------------
    // Dependencies & Variables
    // -------------------------------------------------------------------------
    @Inject lateinit var controller: MapController
    private lateinit var cropViewModel: CropViewModel

    // UI Elements
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var appLogo: TextView
    private lateinit var layoutMapOffline: LinearLayout
    private lateinit var btnRetryMap: Button
    private lateinit var btnConfirm: Button
    private lateinit var layoutSearchNav: LinearLayout
    private lateinit var btnPrevResult: ImageButton
    private lateinit var btnNextResult: ImageButton
    private lateinit var tvResultCount: TextView

    // State Variables
    private var googleMap: GoogleMap? = null
    private var isSearchOpen = false
    private var selectedLatLng: LatLng? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var searchResults: List<CropEntity> = emptyList()
    private var currentSearchIndex = 0
    private   val philippinesBounds = com.google.android.gms.maps.model.LatLngBounds(
        LatLng(4.215806, 116.809228),
        LatLng(21.321798, 126.605335)
    )

    // -------------------------------------------------------------------------
    // Lifecycle Methods
    // -------------------------------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)

        // 1. Initialize ViewModel & Controller
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        controller.bindView(this)

        // 2. Initialize Views
        initViews(view)

        // 3. Setup Listeners
        setupListeners()
        setupSearchLogic()

        // 4. Initialize Map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // 5. Initial Network Check
        checkNetworkAndLoad()

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshMapState()
        checkNetworkAndLoad()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshMapState()
        } else {
            if (cropViewModel.isMapPickerMode) {
                cropViewModel.isMapPickerMode = false
            }
        }
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Map Setup & Callbacks
    // -------------------------------------------------------------------------
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.setLatLngBoundsForCameraTarget(philippinesBounds)
        map.setMinZoomPreference(5.5f)
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        // Enable MyLocation Layer if permission granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
        }

        controller.setupObserver(viewLifecycleOwner)

        // Map Click Listener (For Picking Location)
        map.setOnMapClickListener { latLng ->
            if (cropViewModel.isMapPickerMode) {
                map.clear()
                map.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
                selectedLatLng = latLng
                btnConfirm.visibility = View.VISIBLE
            } else {
                selectedLatLng = null
            }
        }

        // Marker Click (For viewing details)
        map.setOnMarkerClickListener { marker ->
            if (!cropViewModel.isMapPickerMode) {
                marker.showInfoWindow()
                true
            } else {
                false
            }
        }

        // Info Window Click (Navigate to details)
        map.setOnInfoWindowClickListener { marker ->
            if (!cropViewModel.isMapPickerMode) {
                val crop = marker.tag as? CropEntity
                if (crop != null) {
                    controller.onCropMarkerClicked(crop, cropViewModel)
                }
            }
        }

        refreshMapState()
    }

    // -------------------------------------------------------------------------
    // Initialization Helpers
    // -------------------------------------------------------------------------
    private fun initViews(view: View) {
        searchInput = view.findViewById(R.id.search_input)
        searchButton = view.findViewById(R.id.search_button)
        menuButton = view.findViewById(R.id.menu_button)
        appLogo = view.findViewById(R.id.app_logo_text)
        layoutMapOffline = view.findViewById(R.id.layoutMapOffline)
        btnRetryMap = view.findViewById(R.id.btnRetryMap)
        btnConfirm = view.findViewById(R.id.btnConfirmLocation)
        layoutSearchNav = view.findViewById(R.id.layoutSearchNav)
        btnPrevResult = view.findViewById(R.id.btnPrevResult)
        btnNextResult = view.findViewById(R.id.btnNextResult)
        tvResultCount = view.findViewById(R.id.tvResultCount)
    }

    private fun setupListeners() {
        menuButton.setOnClickListener { (activity as? MainActivity)?.openDrawer() }

        btnRetryMap.setOnClickListener { checkNetworkAndLoad() }

        btnConfirm.setOnClickListener {
            if (selectedLatLng != null) {
                if (philippinesBounds.contains(selectedLatLng!!)) {
                    cropViewModel.pickLocation(selectedLatLng!!.latitude, selectedLatLng!!.longitude)
                    cropViewModel.isMapPickerMode = false
                    (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)

                } else {
                    showToast("Please select a location inside the Philippines.")
                }
            } else {
                showToast("Please tap the map to pin a location first.")
            }
        }
        btnPrevResult.setOnClickListener {
            if (searchResults.isNotEmpty()) {
                if (currentSearchIndex > 0) {
                    currentSearchIndex--
                    focusOnCurrentResult()
                } else {

                    currentSearchIndex = searchResults.lastIndex
                    focusOnCurrentResult()
                }
            }
        }

        btnNextResult.setOnClickListener {
            if (searchResults.isNotEmpty()) {
                if (currentSearchIndex < searchResults.lastIndex) {
                    currentSearchIndex++
                    focusOnCurrentResult()
                } else {
                    // Optional: Loop back to start?
                    currentSearchIndex = 0
                    focusOnCurrentResult()
                }
            }
        }
    }

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

    // -------------------------------------------------------------------------
    // Core Map Logic (State & Display)
    // -------------------------------------------------------------------------
    private fun checkNetworkAndLoad() {
        layoutMapOffline.visibility = View.GONE

        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            if (googleMap != null) {
                refreshMapState()
            }
        } else {
            showToast("Offline Mode: Showing cached map data.")
        }
    }

    private fun refreshMapState() {
        val map = googleMap ?: return

        if (cropViewModel.isMapPickerMode) {
            // MODE: Picking a Location
            appLogo.visibility = View.GONE
            searchButton.visibility = View.GONE
            searchInput.visibility = View.GONE
            btnConfirm.visibility = View.GONE
            map.clear()
            selectedLatLng = null

            showToast("Tap on the map to select location")
            moveToUserLocation()

        } else {
            // MODE: Viewing Crops
            appLogo.visibility = View.VISIBLE
            searchButton.visibility = View.VISIBLE
            btnConfirm.visibility = View.GONE

            controller.forceRefreshCrops()
            controller.onSearchQueryChanged("")

            val targetCrop = cropViewModel.cropToFocus
            if (targetCrop != null && targetCrop.latitude != 0.0) {
                val location = LatLng(targetCrop.latitude, targetCrop.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                cropViewModel.cropToFocus = null
            } else {
                moveToUserLocation()
            }
        }
    }

    override fun displayCropsOnMap(crops: List<CropEntity>) {
        val map = googleMap ?: return
        if (cropViewModel.isMapPickerMode) return

        map.clear()
        val markerMap = mutableMapOf<Int, com.google.android.gms.maps.model.Marker>()

        for (crop in crops) {
            if (crop.latitude != 0.0 && crop.longitude != 0.0) {
                val position = LatLng(crop.latitude, crop.longitude)
                val hue = when {
                    isReadyToHarvest(crop) -> BitmapDescriptorFactory.HUE_ORANGE
                    isScheduled(crop) -> BitmapDescriptorFactory.HUE_BLUE
                    isOverdue(crop) -> BitmapDescriptorFactory.HUE_RED
                    else -> BitmapDescriptorFactory.HUE_GREEN
                }

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(crop.cropName)
                        .snippet(getCropStatusString(crop))
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                )
                marker?.tag = crop
                if (marker != null) {
                    markerMap[crop.id] = marker
                }
            }
        }
        val query = searchInput.text.toString().trim()
        if (query.isNotEmpty()) {
            searchResults = crops.filter {
                it.cropName.contains(query, ignoreCase = true) && it.latitude != 0.0
            }
        } else {
            searchResults = emptyList()
        }
        if (searchResults.isNotEmpty()) {
            currentSearchIndex = 0
            layoutSearchNav.visibility = View.VISIBLE
            updateNavText()
            focusOnCurrentResult(markerMap)
        } else {
            layoutSearchNav.visibility = View.GONE
        }
    }

    private fun moveToUserLocation() {
        val philippines = LatLng(12.8797, 121.7740)
        val defaultZoom = 6f

        // Check Permissions
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(philippines, defaultZoom))
            return
        }

        // Get Location
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
            } else {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(philippines, defaultZoom))
            }
        }.addOnFailureListener {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(philippines, defaultZoom))
        }
    }

    // -------------------------------------------------------------------------
    // UI Helpers (Search, Toasts, Formatting)
    // -------------------------------------------------------------------------
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
        layoutSearchNav.visibility = View.GONE
        appLogo.visibility = View.VISIBLE
        searchInput.visibility = View.GONE
        searchButton.setImageResource(android.R.drawable.ic_menu_search)
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
        controller.onSearchQueryChanged("")
    }

    private fun getCropStatusString(crop: CropEntity): String {
        return when {
            isReadyToHarvest(crop) -> "Ready! Harvest: ${crop.mindate?.let { dateFormat.format(Date(it)) }}"
            isScheduled(crop) -> "Plant Schedule: ${dateFormat.format(Date(crop.date))}"
            isOverdue(crop) -> "Overdue"
            else -> "Growing"
        }
    }

    private fun isReadyToHarvest(crop: CropEntity): Boolean {
        val today = System.currentTimeMillis()
        val min = crop.mindate ?: Long.MAX_VALUE
        return today >= min && crop.harvestedDate == null
    }
    private fun updateNavText() {
        val count = searchResults.size
        val current = currentSearchIndex + 1
        tvResultCount.text = "$current / $count"
    }

    private fun focusOnCurrentResult(existingMarkers: Map<Int, com.google.android.gms.maps.model.Marker>? = null) {
        if (searchResults.isEmpty()) return

        val crop = searchResults[currentSearchIndex]
        val target = LatLng(crop.latitude, crop.longitude)
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 15f))

        updateNavText()
        if (existingMarkers != null) {
            existingMarkers[crop.id]?.showInfoWindow()
        }
    }

    private fun isScheduled(crop: CropEntity): Boolean = System.currentTimeMillis() < crop.date

    private fun isOverdue(crop: CropEntity): Boolean =
        (crop.maxdate ?: 0L) != 0L && System.currentTimeMillis() > crop.maxdate!! && crop.harvestedDate == null

    // -------------------------------------------------------------------------
    // View Interface Implementation
    // -------------------------------------------------------------------------
    override fun navigateToCropDetails() {
        cropViewModel.lastSourceId = R.id.nav_map
        (activity as? MainActivity)?.switchFragment(MainActivity.NAV_CROP_DETAILS)
    }

    override fun showToast(message: String) {
        (activity as? MainActivity)?.showToast(message, isError = false)
    }

    override fun getFragmentContext(): Context = requireContext()
}