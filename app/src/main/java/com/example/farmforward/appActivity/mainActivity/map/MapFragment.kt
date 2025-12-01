package com.example.farmforward.appActivity.mainActivity.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
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

    @Inject lateinit var controller: MapController
    private lateinit var cropViewModel: CropViewModel
    private var googleMap: GoogleMap? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Search UI
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var appLogo: ImageView
    private var isSearchOpen = false

    private lateinit var layoutMapOffline: LinearLayout
    private lateinit var btnRetryMap: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        searchInput = view.findViewById(R.id.search_input)
        searchButton = view.findViewById(R.id.search_button)
        menuButton = view.findViewById(R.id.menu_button)
        appLogo = view.findViewById(R.id.app_logo)
        layoutMapOffline = view.findViewById(R.id.layoutMapOffline)
        btnRetryMap = view.findViewById(R.id.btnRetryMap)

        controller.bindView(this)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupSearchLogic()

        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        btnRetryMap.setOnClickListener {
            layoutMapOffline.visibility = View.GONE
        }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            moveToUserLocation()
        }

        map.setOnMarkerClickListener { marker ->
            marker.showInfoWindow()
            true
        }

        map.setOnInfoWindowClickListener { marker ->
            val crop = marker.tag as? CropEntity
            if (crop != null) {
                controller.onCropMarkerClicked(crop, cropViewModel)
            }
        }

        controller.setupObserver(viewLifecycleOwner)
    }

    override fun displayCropsOnMap(crops: List<CropEntity>) {
        val map = googleMap ?: return
        map.clear()

        for (crop in crops) {
            if (crop.latitude != 0.0 && crop.longitude != 0.0) {
                val position = LatLng(crop.latitude, crop.longitude)

                val hue = when {
                    isReadyToHarvest(crop) -> BitmapDescriptorFactory.HUE_ORANGE
                    isScheduled(crop) -> BitmapDescriptorFactory.HUE_BLUE
                    isOverdue(crop) -> BitmapDescriptorFactory.HUE_RED
                    else -> BitmapDescriptorFactory.HUE_GREEN
                }

                val statusText = getCropStatusString(crop)

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(crop.cropName)
                        .snippet(statusText)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                )
                marker?.tag = crop
            }
        }
    }

    private fun getCropStatusString(crop: CropEntity): String {
        return when {
            isReadyToHarvest(crop) -> {
                val dateStr = crop.mindate?.let { dateFormat.format(Date(it)) } ?: "N/A"
                "Ready! Harvest: $dateStr"
            }
            isScheduled(crop) -> {
                val dateStr = dateFormat.format(Date(crop.date))
                "Plant Schedule: $dateStr"
            }
            isOverdue(crop) -> {
                val dateStr = crop.mindate?.let { dateFormat.format(Date(it)) } ?: "N/A"
                "Overdue (Est: $dateStr)"
            }
            else -> {
                val dateStr = crop.mindate?.let { dateFormat.format(Date(it)) } ?: "N/A"
                "Growing (Est Harvest: $dateStr)"
            }
        }
    }

    private fun isReadyToHarvest(crop: CropEntity): Boolean {
        val today = System.currentTimeMillis()
        val min = crop.mindate ?: Long.MAX_VALUE
        return today >= min && crop.harvestedDate == null
    }

    private fun isScheduled(crop: CropEntity): Boolean {
        return System.currentTimeMillis() < crop.date
    }

    private fun isOverdue(crop: CropEntity): Boolean {
        val today = System.currentTimeMillis()
        val max = crop.maxdate ?: 0L
        return max != 0L && today > max && crop.harvestedDate == null
    }

    private fun moveToUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
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

    override fun navigateToCropDetails() {
        cropViewModel.lastSourceId = R.id.nav_map
        (activity as? MainActivity)?.switchFragment(MainActivity.NAV_CROP_DETAILS)
    }

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun getFragmentContext(): Context = requireContext()

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }
}