package com.example.farmforward.appActivity.mainActivity.calc

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.dataclass.CropFormDraft
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager
import com.example.farmforward.database.staticData.CropRepository
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.loadingUtils.LoadingDialogFragment
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.otherUtils.SquareButton
import com.example.farmforward.utils.otherUtils.handleKeyboardVisibility
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CalcFragment : Fragment(), CalcView {

    @Inject lateinit var controller: CalcController
    @Inject lateinit var syncManager: FirebaseSyncManager
    @Inject lateinit var weatherRepository: WeatherRepository
    @Inject lateinit var cropRepository: CropRepository

    private lateinit var cropViewModel: CropViewModel
    private var validCropNames: List<String> = emptyList()
    private lateinit var menuButton: ImageButton
    private lateinit var inputCrop: AutoCompleteTextView
    private lateinit var inputArea: EditText
    private lateinit var inputSoilType: AutoCompleteTextView
    private lateinit var inputIrrigation: AutoCompleteTextView
    private lateinit var inputPlantDensity: AutoCompleteTextView
    private lateinit var inputFertilizer: AutoCompleteTextView
    private lateinit var inputWeather: EditText
    private lateinit var inputRegion: EditText
    private lateinit var btnCalculate: Button
    private lateinit var btnCancel: Button

    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0
    private var selectedDateMillis: Long = System.currentTimeMillis()

    // Class level variable
    private var loadingDialog: LoadingDialogFragment? = null

    private var rawWeatherCondition: String = "Normal"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calc, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        controller.bindView(this, cropViewModel, lifecycleScope)

        inputCrop = view.findViewById(R.id.inputCrop)
        inputArea = view.findViewById(R.id.inputArea)
        inputSoilType = view.findViewById(R.id.inputSoilType)
        inputIrrigation = view.findViewById(R.id.inputIrrigationLevel)
        inputPlantDensity = view.findViewById(R.id.inputPlantDensity)
        inputFertilizer = view.findViewById(R.id.inputFertilizerUsed)
        inputWeather = view.findViewById(R.id.inputWeather)
        inputRegion = view.findViewById(R.id.inputRegion)
        menuButton = view.findViewById(R.id.menu_button)
        btnCalculate = view.findViewById(R.id.btnCalculate)
        btnCancel = view.findViewById(R.id.btnCancel)

        btnCancel.setOnClickListener { controller.onCancelClicked() }

        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val calendarGrid = view.findViewById<GridLayout>(R.id.calendarGrid)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val rootScroll = view.findViewById<ScrollView>(R.id.rootLayout)
        rootScroll?.handleKeyboardVisibility()

        setupInputFields()
        setupKeyboardLogic()
        inputRegion.setOnClickListener {
            hideKeyboard(it)
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                openMapPicker()
            } else {
                (activity as? MainActivity)?.showToast("Map location requires Internet.", isError = true)
            }
        }
        if (cropViewModel.tempDate != null) {
            selectedDateMillis = cropViewModel.tempDate!!
        }

        cropViewModel.pickedLocation.observe(viewLifecycleOwner) { latLng ->
            if (latLng != null) {
                if (latLng.latitude != 0.0 && latLng.longitude != 0.0) {
                    selectedLat = latLng.latitude
                    selectedLng = latLng.longitude
                    val address = getAddressName(selectedLat, selectedLng)
                    inputRegion.setText(address)
                    fetchSpecificWeather(selectedLat, selectedLng)
                    saveFormState()
                }
                cropViewModel.clearPickedLocation()
            }
        }

        lifecycleScope.launch { controller.onViewCreated() }

        inputCrop.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val cropName = parent.adapter.getItem(position) as? String ?: ""
            lifecycleScope.launch { controller.onCropSelected(cropName) }
        }

        btnCalculate.setOnClickListener {
            val weatherOption = mapWeatherToOption(rawWeatherCondition)

            controller.onCalculateClicked(
                cropName = inputCrop.text.toString().trim(),
                areaStr = inputArea.text.toString().trim(),
                soilSel = inputSoilType.text.toString().trim(),
                irrSel = inputIrrigation.text.toString().trim(),
                denSel = inputPlantDensity.text.toString().trim(),
                fertSel = inputFertilizer.text.toString().trim(),
                weatherSel = weatherOption,
                selectedDateMillis = selectedDateMillis
            )
        }

        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        val calendar = Calendar.getInstance()
        updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())

        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }
        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }

        if (cropViewModel.formDraft != null) {
            restoreFormState(cropViewModel.formDraft!!)
        }
        setupUI(view)
        return view
    }

    private fun mapWeatherToOption(condition: String): String {
        return when {
            condition.contains("Rain", ignoreCase = true) ||
                    condition.contains("Drizzle", ignoreCase = true) ||
                    condition.contains("Thunderstorm", ignoreCase = true) -> "Wet/Excess"

            condition.contains("Ash", ignoreCase = true) ||
                    condition.contains("Dust", ignoreCase = true) ||
                    condition.contains("Drought", ignoreCase = true) -> "Dry/Drought"

            else -> "Normal"
        }
    }

    private fun setupUI(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    hideKeyboard(v)
                    requireActivity().currentFocus?.clearFocus()
                }
                false
            }
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val innerView = view.getChildAt(i)
                setupUI(innerView)
            }
        }
    }

    private fun setupKeyboardLogic() {
        inputArea.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard(v)
                v.clearFocus()
                true
            } else {
                false
            }
        }
        inputCrop.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                hideKeyboard(v)
                v.clearFocus()
                inputCrop.dismissDropDown()
                true
            } else {
                false
            }
        }
    }

    private fun fetchSpecificWeather(lat: Double, lng: Double) {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            inputWeather.setText("Offline / Unavailable")
            rawWeatherCondition = "Normal"
            return
        }
        inputWeather.setText("Loading...")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.WEATHER_API_KEY
                val response = RetrofitClient.instance.getForecastByCoordinates(lat, lng, apiKey)
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    if (response.isSuccessful && response.body() != null) {
                        val forecast = response.body()!!.list.firstOrNull()
                        if (forecast != null) {
                            val condition = forecast.weather.firstOrNull()?.main ?: "Clear"
                            rawWeatherCondition = condition
                            val tempVal = forecast.main.temp
                            val temp = String.format("%.1f°C", tempVal)
                            inputWeather.setText("$condition, $temp")
                        } else {
                            inputWeather.setText("No data")
                            rawWeatherCondition = "Normal"
                        }
                    } else {
                        inputWeather.setText("Weather Error")
                        rawWeatherCondition = "Normal"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        inputWeather.setText("Network Error")
                        rawWeatherCondition = "Normal"
                    }
                }
            }
        }
    }

    override fun preFillForm(crop: CropEntity) {
        inputCrop.setText(crop.cropName, false)
        inputArea.setText(crop.area.toString())
        inputSoilType.setText(crop.soilType, false)
        inputIrrigation.setText(crop.irrigationLevel, false)
        inputPlantDensity.setText(crop.plantDensity, false)
        inputFertilizer.setText(crop.fertilizerUsed, false)
        if (crop.weatherCondition != null) {
            rawWeatherCondition = crop.weatherCondition
            inputWeather.setText(crop.weatherCondition)
        }

        if (selectedLat == 0.0 && selectedLng == 0.0) {
            selectedLat = crop.latitude
            selectedLng = crop.longitude
            if (selectedLat != 0.0) {
                inputRegion.setText(getAddressName(selectedLat, selectedLng))
                fetchSpecificWeather(selectedLat, selectedLng)
            } else {
                inputRegion.hint = "Tap to add location"
            }
        }
        selectedDateMillis = crop.date
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateMillis
        val calendarGrid = view?.findViewById<GridLayout>(R.id.calendarGrid)
        val tvMonthYear = view?.findViewById<TextView>(R.id.tvMonthYear)
        if(calendarGrid != null && tvMonthYear != null) {
            updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }
    }

    override fun setButtonText(text: String) { btnCalculate.text = text }

    private fun isCropSelected(): Boolean {
        val input = inputCrop.text.toString().trim()
        if (input.isEmpty()) {
            (activity as? MainActivity)?.showToast("Please select a crop first.", isError = true)
            return false
        }
        val exists = validCropNames.any { it.equals(input, ignoreCase = true) }
        if (!exists) {
            (activity as? MainActivity)?.showToast("Crop not found! Wait for future updates.", isError = true)
            return false
        }
        return true
    }

    private fun setupInputFields() {
        val dropdowns = listOf(inputSoilType, inputIrrigation, inputPlantDensity, inputFertilizer)

        dropdowns.forEach { input ->
            input.inputType = InputType.TYPE_NULL
            input.keyListener = null
            input.isCursorVisible = false
            input.showSoftInputOnFocus = false

            input.setOnClickListener {
                hideKeyboard(it)
                if (isCropSelected()) {
                    input.showDropDown()
                }
            }

            input.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(view)
                    if (isCropSelected()) {
                        input.showDropDown()
                    } else {
                        input.clearFocus()
                    }
                }
            }
        }
        inputWeather.inputType = InputType.TYPE_NULL
        inputWeather.keyListener = null
        inputWeather.isFocusable = false
        inputWeather.isClickable = false
        inputWeather.isCursorVisible = false

        inputRegion.inputType = InputType.TYPE_NULL
        inputRegion.keyListener = null
        inputRegion.isFocusable = false
        inputRegion.isCursorVisible = false

        inputRegion.setOnClickListener {
            hideKeyboard(it)
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                openMapPicker()
            } else {
                (activity as? MainActivity)?.showToast("Map location requires Internet.", isError = true)
            }
        }
    }
    private fun openMapPicker() {
        saveFormState()
        cropViewModel.isMapPickerMode = true
        (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_map)
        (activity as? MainActivity)?.showToast("Tap the map to pin a location", isError = false)
    }

    private fun getAddressName(lat: Double, lng: Double): String {
        return try {
            val geocoder = android.location.Geocoder(requireContext(), java.util.Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].locality ?: addresses[0].adminArea ?: "Selected Location"
            } else {
                "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
            }
        } catch (e: Exception) {
            "Selected Location"
        }
    }

    override fun clearFactorInputs() {
        inputSoilType.setText("", false)
        inputIrrigation.setText("", false)
        inputPlantDensity.setText("", false)
        inputFertilizer.setText("", false)
    }

    override fun clearAllInputs() {
        inputCrop.setText("", false)
        val adapter = inputCrop.adapter as? ArrayAdapter<String>
        adapter?.filter?.filter(null)
        inputArea.setText("")
        clearFactorInputs()
        inputWeather.setText("")
        inputRegion.setText("")
        selectedLat = 0.0
        selectedLng = 0.0
        cropViewModel.clearDraft()
        cropViewModel.tempDate = null
        selectedDateMillis = System.currentTimeMillis()
        rawWeatherCondition = "Normal"
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun updateCalendar(
        calendar: Calendar,
        calendarGrid: GridLayout,
        tvMonthYear: TextView,
        context: Context
    ) {
        calendarGrid.removeAllViews()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthFormat.format(calendar.time)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val todayCal = Calendar.getInstance()
        val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

        val paddingPx = (2 * context.resources.displayMetrics.density).toInt()
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val maxSize = (screenWidth / 9f).toInt()

        for (day in 1..daysInMonth) {
            val btnDay = SquareButton(context).apply {
                text = day.toString()
                isAllCaps = false
                includeFontPadding = false
                gravity = Gravity.CENTER
                textSize = maxSize / 6f
                setBackgroundResource(R.drawable.day_button_selector)
            }
            val isToday = year == todayCal.get(Calendar.YEAR)
                    && month == todayCal.get(Calendar.MONTH)
                    && day == todayCal.get(Calendar.DAY_OF_MONTH)
            val isSelected = year == selectedCal.get(Calendar.YEAR)
                    && month == selectedCal.get(Calendar.MONTH)
                    && day == selectedCal.get(Calendar.DAY_OF_MONTH)

            when {
                isSelected -> btnDay.setTextColor(ContextCompat.getColor(context, R.color.tan))
                isToday -> btnDay.setTextColor(ContextCompat.getColor(context, R.color.kombuGreen))
                else -> btnDay.setTextColor(ContextCompat.getColorStateList(context, R.color.day_text_color))
            }
            btnDay.isSelected = isSelected
            val params = GridLayout.LayoutParams().apply {
                width = maxSize
                height = maxSize
                setMargins(4, 4, 4, 4)
                setGravity(Gravity.CENTER)
            }
            btnDay.layoutParams = params
            btnDay.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            btnDay.setOnClickListener {
                selectedDateMillis = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                updateCalendar(calendar, calendarGrid, tvMonthYear, context)
            }
            calendarGrid.addView(btnDay)
        }
    }

    override fun showToast(message: String, isError: Boolean) {
        (activity as? MainActivity)?.showToast(message, isError)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun setCropAdapter(cropNames: List<String>) {
        validCropNames = cropNames
        val adapter = ArrayAdapter(requireContext(), R.layout.item_crop_dropdown, cropNames)
        inputCrop.setAdapter(adapter)
        inputCrop.threshold = 1
        inputCrop.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && inputCrop.text.isNotEmpty()) {
                inputCrop.showDropDown()
            }
        }
        inputCrop.setOnClickListener {
            if (inputCrop.text.isNotEmpty()) {
                inputCrop.showDropDown()
            }
        }
    }

    override fun setFactorAdapters(
        soilOptions: List<String>,
        irrigationOptions: List<String>,
        densityOptions: List<String>,
        fertOptions: List<String>
    ) {
        val soilAdapter = ArrayAdapter(requireContext(), R.layout.item_crop_dropdown, soilOptions)
        val irrigationAdapter = ArrayAdapter(requireContext(),  R.layout.item_crop_dropdown, irrigationOptions)
        val densityAdapter = ArrayAdapter(requireContext(),  R.layout.item_crop_dropdown, densityOptions)
        val fertilizerAdapter = ArrayAdapter(requireContext(),  R.layout.item_crop_dropdown, fertOptions)

        inputSoilType.setAdapter(soilAdapter)
        inputIrrigation.setAdapter(irrigationAdapter)
        inputPlantDensity.setAdapter(densityAdapter)
        inputFertilizer.setAdapter(fertilizerAdapter)
    }

    override fun getCurrentLocation(onLocationFound: (Double, Double) -> Unit) {
        if (selectedLat != 0.0 && selectedLng != 0.0) {
            onLocationFound(selectedLat, selectedLng)
            return
        }
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            onLocationFound(0.0, 0.0)
            return
        }
        val mainActivity = requireActivity() as MainActivity
        if (mainActivity.controller.hasLocationPermission()) {
            mainActivity.controller.fetchCurrentLocation(mainActivity) { lat, lng ->
                onLocationFound(lat, lng)
            }
        } else {
            onLocationFound(0.0, 0.0)
        }
    }

    // 1. UPDATED: No 'val', uses class property. No Coroutine.
    override fun navigateToLoading(isOnline: Boolean) {
        loadingDialog = LoadingDialogFragment()
        loadingDialog?.isCancelable = false
        loadingDialog?.show(parentFragmentManager, "LoadingDialog")
    }

    // 2. UPDATED: Allows controller to update text
    override fun updateLoading(progress: Int, message: String) {
        if (loadingDialog?.isAdded == true) {
            loadingDialog?.updateProgress(progress, message)
        }
    }

    // 3. UPDATED: Dismisses the dialog
    override fun onCalculationSuccess() {
        if (loadingDialog?.isAdded == true) {
            loadingDialog?.dismiss()
        }
        clearAllInputs()
        (requireActivity() as MainActivity).navigateToGrowthResult()
    }

    private fun saveFormState() {
        val draft = CropFormDraft(
            name = inputCrop.text.toString(),
            area = inputArea.text.toString(),
            soil = inputSoilType.text.toString(),
            irrigation = inputIrrigation.text.toString(),
            density = inputPlantDensity.text.toString(),
            fertilizer = inputFertilizer.text.toString(),
            lat = selectedLat,
            lng = selectedLng
        )
        cropViewModel.formDraft = draft
        cropViewModel.tempDate = selectedDateMillis
    }

    private fun restoreFormState(draft: CropFormDraft) {
        if (draft.name.isNotEmpty()) inputCrop.setText(draft.name, false)
        if (draft.area.isNotEmpty()) inputArea.setText(draft.area)
        if (draft.soil.isNotEmpty()) inputSoilType.setText(draft.soil, false)
        if (draft.irrigation.isNotEmpty()) inputIrrigation.setText(draft.irrigation, false)
        if (draft.density.isNotEmpty()) inputPlantDensity.setText(draft.density, false)
        if (draft.fertilizer.isNotEmpty()) inputFertilizer.setText(draft.fertilizer, false)

        if (draft.lat != 0.0) {
            selectedLat = draft.lat
            selectedLng = draft.lng
            inputRegion.setText(getAddressName(selectedLat, selectedLng))
            fetchSpecificWeather(selectedLat, selectedLng)
        }
        if (cropViewModel.tempDate != null) {
            selectedDateMillis = cropViewModel.tempDate!!
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDateMillis
            val calendarGrid = view?.findViewById<GridLayout>(R.id.calendarGrid)
            val tvMonthYear = view?.findViewById<TextView>(R.id.tvMonthYear)
            if (calendarGrid != null && tvMonthYear != null) {
                updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
            }
            cropViewModel.tempDate = null
        }
    }
}