package com.example.farmforward.appActivity.calc

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.firebase.FirebaseSyncManager
import com.example.farmforward.utils.LoadingDialogFragment
import com.example.farmforward.utils.WeatherRepository
import com.example.farmforward.viewModel.CropViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import javax.inject.Inject


@AndroidEntryPoint
class CalcFragment : Fragment(), CalcView {

    @Inject lateinit var controller: CalcController
    @Inject lateinit var syncManager: FirebaseSyncManager
    @Inject lateinit var weatherRepository: WeatherRepository

    private lateinit var cropViewModel: CropViewModel

    // UI Elements
    private lateinit var inputCrop: AutoCompleteTextView
    private lateinit var inputArea: EditText
    private lateinit var inputSoilType: AutoCompleteTextView
    private lateinit var inputIrrigation: AutoCompleteTextView
    private lateinit var inputPlantDensity: AutoCompleteTextView
    private lateinit var inputFertilizer: AutoCompleteTextView
    private lateinit var inputWeather: EditText
    private lateinit var inputRegion: EditText

    private var selectedDateMillis: Long = System.currentTimeMillis()

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

        setupInputFields()

        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)
        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val calendarGrid = view.findViewById<GridLayout>(R.id.calendarGrid)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)

        lifecycleScope.launch { controller.onViewCreated() }

        inputCrop.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val cropName = parent.adapter.getItem(position) as? String ?: ""
            lifecycleScope.launch { controller.onCropSelected(cropName) }
        }

        btnCalculate.setOnClickListener {
            controller.onCalculateClicked(
                cropName = inputCrop.text.toString().trim(),
                areaStr = inputArea.text.toString().trim(),
                soilSel = inputSoilType.text.toString().trim(),
                irrSel = inputIrrigation.text.toString().trim(),
                denSel = inputPlantDensity.text.toString().trim(),
                fertSel = inputFertilizer.text.toString().trim(),
                selectedDateMillis = selectedDateMillis
            )
        }

        val calendar = Calendar.getInstance()
        updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())

        loadEnvironmentData()

        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }
        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }

        return view
    }

    private fun setupInputFields() {
        val dropdowns = listOf(inputSoilType, inputIrrigation, inputPlantDensity, inputFertilizer)
        dropdowns.forEach { input ->
            input.inputType = InputType.TYPE_NULL
            input.keyListener = null

            input.setOnClickListener {
                if (isCropSelected()) input.showDropDown()
            }
            input.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(view)
                    if (isCropSelected()) input.showDropDown() else input.clearFocus()
                }
            }
        }

        val readOnlyFields = listOf(inputWeather, inputRegion)
        readOnlyFields.forEach { input ->
            input.inputType = InputType.TYPE_NULL
            input.keyListener = null
            input.isFocusable = false
            input.isClickable = false
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun loadEnvironmentData() {
        weatherRepository.loadCachedData()

        val region = weatherRepository.cachedLocationName
        if (!region.isNullOrEmpty()) {
            inputRegion.setText(region)
        } else {
            inputRegion.hint = "Location unknown"
        }

        val forecasts = weatherRepository.cachedForecasts
        if (!forecasts.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val closestForecast = forecasts.minByOrNull { abs((it.dt * 1000) - now) }

            if (closestForecast != null && !closestForecast.weather.isNullOrEmpty()) {
                val mainCondition = closestForecast.weather[0].main
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                val timeString = timeFormat.format(Date(closestForecast.dt * 1000))

                inputWeather.setText("$mainCondition @ $timeString")
            }
        } else {
            inputWeather.hint = "Weather unavailable"
        }
    }


    private fun isCropSelected(): Boolean {
        val crop = inputCrop.text.toString().trim()
        if (crop.isEmpty()) {
            showCustomSnackbar("Please select a crop first.", isError = true)
            return false
        }
        return true
    }

    override fun setFactorAdapters(
        soilOptions: List<String>,
        irrigationOptions: List<String>,
        densityOptions: List<String>,
        fertOptions: List<String>
    ) {
        val soilAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, soilOptions)
        val irrigationAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, irrigationOptions)
        val densityAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, densityOptions)
        val fertilizerAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, fertOptions)

        inputSoilType.setAdapter(soilAdapter)
        inputIrrigation.setAdapter(irrigationAdapter)
        inputPlantDensity.setAdapter(densityAdapter)
        inputFertilizer.setAdapter(fertilizerAdapter)
    }

    private fun updateCalendar(calendar: Calendar, calendarGrid: GridLayout, tvMonthYear: TextView, context: Context) {
        calendarGrid.removeAllViews()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthFormat.format(calendar.time)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val todayCal = Calendar.getInstance()
        val selectedCal = Calendar.getInstance()
        selectedCal.timeInMillis = selectedDateMillis

        for (day in 1..daysInMonth) {
            val btnDay = Button(context)
            btnDay.text = day.toString()
            btnDay.textSize = 14f
            btnDay.setBackgroundResource(R.drawable.day_button_selector)
            val isToday = (year == todayCal.get(Calendar.YEAR) &&
                    month == todayCal.get(Calendar.MONTH) &&
                    day == todayCal.get(Calendar.DAY_OF_MONTH))
            val isSelectedDate = (year == selectedCal.get(Calendar.YEAR) &&
                    month == selectedCal.get(Calendar.MONTH) &&
                    day == selectedCal.get(Calendar.DAY_OF_MONTH))
            if (isToday && !isSelectedDate) {
                btnDay.setTextColor(ContextCompat.getColor(context, R.color.kombuGreen))
                btnDay.typeface = android.graphics.Typeface.DEFAULT_BOLD
            } else {
                btnDay.setTextColor(ContextCompat.getColorStateList(context, R.color.day_text_color))
            }
            btnDay.isSelected = isSelectedDate
            btnDay.layoutParams = GridLayout.LayoutParams().apply {
                width = 110
                height = 110
                setMargins(8, 8, 8, 8)
            }
            btnDay.setOnClickListener {
                for (i in 0 until calendarGrid.childCount) calendarGrid.getChildAt(i).isSelected = false
                btnDay.isSelected = true
                val newSelected = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                this@CalcFragment.selectedDateMillis = newSelected.timeInMillis
            }
            calendarGrid.addView(btnDay)
        }
    }

    override fun showToast(message: String, duration: Int) {
        showCustomSnackbar(message, isError = false)
    }

    override fun setCropAdapter(cropNames: List<String>) {
        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, cropNames)
        inputCrop.setAdapter(adapter)
    }

    override fun clearFactorInputs() {
        inputSoilType.setText("", false)
        inputIrrigation.setText("", false)
        inputPlantDensity.setText("", false)
        inputFertilizer.setText("", false)
    }

    override fun clearAllInputs() {
        inputCrop.setText("", false)
        inputArea.setText("")
        clearFactorInputs()
        inputWeather.setText("")
        inputRegion.setText("")
    }

    override fun navigateToLoading() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(parentFragmentManager, "LoadingDialog")
        lifecycleScope.launch(Dispatchers.IO) {
            fun updateUi(progress: Int, msg: String) {
                launch(Dispatchers.Main) { loadingDialog.updateProgress(progress, msg) }
            }
            updateUi(10, "Connecting...")
            delay(300)
            updateUi(40, "Syncing Profile...")
            syncManager.syncUsers()
            updateUi(70, "Downloading Crop Data...")
            syncManager.syncCrops()
            updateUi(90, "Finalizing...")
            delay(400)
            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                (requireActivity() as com.example.farmforward.appActivity.mainActivity.MainActivity).navigateToGrowthResult()
            }
        }
    }


    private fun showCustomSnackbar(message: String, isError: Boolean = false) {
        val rootView = view ?: return
        val context = requireContext()
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbarView.backgroundTintList = null

        val borderDrawable = GradientDrawable()
        borderDrawable.shape = GradientDrawable.RECTANGLE
        borderDrawable.cornerRadius = 12f.dpToPx(context)

        if (isError) {
            borderDrawable.setColor(ContextCompat.getColor(context, R.color.tan))
            borderDrawable.setStroke(4, ContextCompat.getColor(context, R.color.kombuGreen))

            snackbar.setTextColor(ContextCompat.getColor(context, R.color.kombuGreen))
            snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.kombuGreen))
        } else {
            borderDrawable.setColor(ContextCompat.getColor(context, R.color.tan))
            borderDrawable.setStroke(4, ContextCompat.getColor(context, R.color.kombuGreen))

            snackbar.setTextColor(ContextCompat.getColor(context, R.color.kombuGreen))
            snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.kombuGreen))
        }

        snackbarView.background = borderDrawable

        snackbar.setAction("OK") { snackbar.dismiss() }

        snackbar.show()
    }
    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

}