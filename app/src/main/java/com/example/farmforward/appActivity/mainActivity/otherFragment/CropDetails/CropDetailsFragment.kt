package com.example.farmforward.appActivity.mainActivity.otherFragment.CropDetails

// ... imports remain the same ...
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class CropDetailsFragment : Fragment(), CropDetailsView {

    @Inject lateinit var controller: CropDetailsController
    private lateinit var cropViewModel: CropViewModel

    // UI Elements
    private lateinit var tvCropName: TextView
    private lateinit var tvArea: TextView
    private lateinit var plantedDate: TextView
    private lateinit var harvestYield: TextView
    private lateinit var imgCrop: ImageView
    private lateinit var tvSoilType: TextView
    private lateinit var tvIrrigation: TextView
    private lateinit var tvDensity: TextView
    private lateinit var tvFertilizer: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvSavedWeather: TextView
    private lateinit var tvRegion: TextView
    private lateinit var btnViewOnMap: Button
    private lateinit var btnHarvest: Button

    // ... Cache State variables ...
    private var cachedRegion: String = ""
    private var cachedLocality: String = ""
    private var isLocationPinned: Boolean = false
    private var currentCropLat: Double? = null
    private var currentCropLon: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_crop_details, container, false)

        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        val tvEdit = view.findViewById<TextView>(R.id.tvEdit)
        val tvDelete = view.findViewById<TextView>(R.id.tvDelete)
        val btnCloseNav = view.findViewById<ImageButton>(R.id.btn_close_nav)

        tvCropName = view.findViewById(R.id.tvCropName)
        tvArea = view.findViewById(R.id.tvArea)
        plantedDate = view.findViewById(R.id.plantedDate)
        harvestYield = view.findViewById(R.id.harvestYield)
        imgCrop = view.findViewById(R.id.etDescription)
        tvSoilType = view.findViewById(R.id.tvSoilType)
        tvIrrigation = view.findViewById(R.id.tvIrrigation)
        tvDensity = view.findViewById(R.id.tvDensity)
        tvFertilizer = view.findViewById(R.id.tvFertilizer)
        tvWeather = view.findViewById(R.id.tvWeather)
        tvRegion = view.findViewById(R.id.tvRegion)
        btnViewOnMap = view.findViewById(R.id.btnViewOnMap)
        btnHarvest = view.findViewById(R.id.btnHarvest)

        // --- IMPORTANT: Ensure you add this ID to your XML layout file ---
        // For now, if you don't have it, create it in fragment_crop_details.xml
        // id: tvSavedWeatherFactor
        tvSavedWeather = view.findViewById(R.id.tvSavedWeatherFactor)
        // ---------------------------------------------------------------

        btnCloseNav.setOnClickListener {
            (activity as? MainActivity)?.controller?.onBackClicked(cropViewModel)
        }
        tvEdit.setOnClickListener { controller.onEditClicked(cropViewModel) }
        tvDelete.setOnClickListener { controller.onDeleteClicked(cropViewModel) }
        btnHarvest.setOnClickListener { controller.onHarvestClicked(cropViewModel) }

        showEmptyState()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.bindView(this)
        controller.setupObserver(viewLifecycleOwner, cropViewModel)
        cropViewModel.cropData.observe(viewLifecycleOwner) { crop ->
            if (crop != null) {
                if (crop.latitude != 0.0 && crop.longitude != 0.0) {
                    currentCropLat = crop.latitude
                    currentCropLon = crop.longitude
                }
            }
        }
    }

    override fun setLocation(region: String, locality: String) {
        cachedRegion = region
        cachedLocality = locality
        updateLocationTextDisplay()
    }

    override fun showMapButton(isVisible: Boolean) {
        btnViewOnMap.visibility = View.VISIBLE
        isLocationPinned = isVisible

        if (isVisible) {
            btnViewOnMap.text = "Show Location on Map"
            btnViewOnMap.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.kombuGreen)
            btnViewOnMap.setOnClickListener {
                val crop = cropViewModel.cropData.value
                if (crop != null) controller.onViewOnMapClicked(cropViewModel, crop)
            }
        } else {
            btnViewOnMap.text = "Add Location"
            btnViewOnMap.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.kombuGreen)
            btnViewOnMap.setOnClickListener { controller.onEditClicked(cropViewModel) }
        }
        updateLocationTextDisplay()
    }

    private fun updateLocationTextDisplay() {
        val hasText = cachedRegion.isNotEmpty() || cachedLocality.isNotEmpty()
        if (hasText) {
            val regionDisplay = if (cachedRegion.isEmpty()) "Unknown Region" else cachedRegion
            if (cachedLocality.isNotEmpty()) {
                tvRegion.text = "$regionDisplay - $cachedLocality"
            } else {
                tvRegion.text = regionDisplay
            }
        } else {
            if (isLocationPinned) {
                tvRegion.text = "Pinned Location"
            } else {
                tvRegion.text = "Location Not Set"
            }
        }
    }


    // --- Setters ---
    override fun setCropName(name: String) { tvCropName.text = name }
    override fun setArea(area: String) { tvArea.text = area }
    override fun setPlantedDate(date: String) { plantedDate.text = date }
    override fun setYield(yield: String) { harvestYield.text = yield }
    override fun setSoil(soil: String) { tvSoilType.text = soil }
    override fun setIrrigation(irrigation: String) { tvIrrigation.text = irrigation }
    override fun setDensity(density: String) { tvDensity.text = density }
    override fun setFertilizer(fertilizer: String) { tvFertilizer.text = fertilizer }

    // Live Weather (e.g., Clear, 30C)
    override fun setWeather(weather: String) { tvWeather.text = weather }

    // NEW: Saved Factor (e.g., Dry/Drought)
    override fun setSavedWeatherFactor(weatherFactor: String) {
        tvSavedWeather.text = weatherFactor
    }

    // ... (rest of methods: setCropImage, navigateToEdit, etc. remain the same) ...
    override fun setCropImage(resourceId: Int) { imgCrop.setImageResource(resourceId) }

    override fun navigateToEdit(crop: CropEntity) {
        (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)
    }

    override fun navigateToMap(crop: CropEntity) {
        (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_map)
    }

    override fun navigateBack(destinationId: Int) {
        (activity as? MainActivity)?.switchFragment(destinationId)
    }

    override fun setCropImageTint(colorRes: Int) {
        imgCrop.setColorFilter(ContextCompat.getColor(requireContext(), colorRes))
    }

    override fun showEmptyState() {
        tvCropName.text = "No Crop Selected"
        tvArea.text = "---"
        btnViewOnMap.visibility = View.GONE
        btnHarvest.visibility = View.GONE
    }

    override fun showDeleteConfirmation(message: String, onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Delete Crop")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }

    override fun showHarvestButton(isVisible: Boolean) {
        btnHarvest.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun showHarvestDatePicker(minHarvestDate: Long, onDateSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                if (selectedCalendar.timeInMillis < minHarvestDate) {
                    showError("Cannot harvest before estimated date.")
                } else {
                    onDateSelected(selectedCalendar.timeInMillis)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.minDate = minHarvestDate
        datePickerDialog.show()
    }

    override fun showError(message: String) {
        (activity as? MainActivity)?.showToast(message, isError = true)
    }

    override fun navigateToGarden() {
        (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_garden)
    }

    override fun getFragmentContext(): Context = requireContext()
    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }
}