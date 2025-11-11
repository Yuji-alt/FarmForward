package com.example.farmforward.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import com.example.farmforward.activityViewmodel.MainActivity
import com.example.farmforward.fragmentController.CalcController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalcFragment : Fragment() {

    private lateinit var controller: CalcController
    private lateinit var cropViewModel: CropViewModel
    private lateinit var inputCrop: AutoCompleteTextView
    private lateinit var inputArea: EditText
    private var currentFactors: Map<String, List<Pair<String, Double>>> = emptyMap()
    private var selectedDateMillis: Long = System.currentTimeMillis() // This is correct

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calc, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        controller = CalcController(requireContext(), cropViewModel)

        // ... (all your findViewById calls) ...
        inputCrop = view.findViewById(R.id.inputCrop)
        inputArea = view.findViewById(R.id.inputArea)
        val inputSoilType = view.findViewById<AutoCompleteTextView>(R.id.inputSoilType)
        val inputIrrigation = view.findViewById<AutoCompleteTextView>(R.id.inputIrrigationLevel)
        val inputPlantDensity = view.findViewById<AutoCompleteTextView>(R.id.inputPlantDensity)
        val inputFertilizer = view.findViewById<AutoCompleteTextView>(R.id.inputFertilizerUsed)
        val inputWeather = view.findViewById<EditText>(R.id.inputWeather)
        val inputRegion = view.findViewById<EditText>(R.id.inputRegion)
        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)
        val tvMonthYear = view.findViewById<TextView>(R.id.tvMonthYear)
        val calendarGrid = view.findViewById<GridLayout>(R.id.calendarGrid)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val cropList = controller.loadCropNames()
        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, cropList)
        inputCrop.setAdapter(adapter)

        inputCrop.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val cropName = adapter.getItem(position) ?: ""
            currentFactors = controller.loadCropFactors(cropName)

            val soilOptions = currentFactors["Soil Type"]?.map { it.first } ?: emptyList()
            val irrigationOptions = currentFactors["Irrigation Level"]?.map { it.first } ?: emptyList()
            val densityOptions = currentFactors["Planting Density"]?.map { it.first } ?: emptyList()
            val fertOptions = currentFactors["Fertilizer Used"]?.map { it.first } ?: emptyList()

            val soilAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, soilOptions)
            val irrigationAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, irrigationOptions)
            val densityAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, densityOptions)
            val fertilizerAdapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, fertOptions)

            inputSoilType.setAdapter(soilAdapter)
            inputIrrigation.setAdapter(irrigationAdapter)
            inputPlantDensity.setAdapter(densityAdapter)
            inputFertilizer.setAdapter(fertilizerAdapter)

            inputSoilType.setText("")
            inputIrrigation.setText("")
            inputPlantDensity.setText("")
            inputFertilizer.setText("")

            val dropdowns = listOf(inputSoilType, inputIrrigation, inputPlantDensity, inputFertilizer)
            dropdowns.forEach { input ->
                input.setOnClickListener { input.showDropDown() }
                input.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) input.showDropDown()
                }
            }
            Toast.makeText(requireContext(), "Factors loaded for $cropName", Toast.LENGTH_SHORT).show()
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


        btnCalculate.setOnClickListener {
            val cropName = inputCrop.text.toString().trim()
            val area = inputArea.text.toString().toDoubleOrNull() ?: 0.0
            val userId = getCurrentUserId()


            if (cropName.isEmpty() || area <= 0.0) {
                Toast.makeText(requireContext(), "Please enter valid crop and area.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val baseYield = controller.getYield(cropName) ?: 0.0
            val soilSel = inputSoilType.text.toString().trim()
            val irrSel = inputIrrigation.text.toString().trim()
            val denSel = inputPlantDensity.text.toString().trim()
            val fertSel = inputFertilizer.text.toString().trim()

            if (soilSel.isEmpty() || irrSel.isEmpty() || denSel.isEmpty() || fertSel.isEmpty()) {
                Toast.makeText(requireContext(), "Please select all factor options first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val soilVal = currentFactors["Soil Type"]?.find { it.first.equals(soilSel, ignoreCase = true) }?.second ?: 0.0
            val irrVal = currentFactors["Irrigation Level"]?.find { it.first.equals(irrSel, ignoreCase = true) }?.second ?: 0.0
            val denVal = currentFactors["Planting Density"]?.find { it.first.equals(denSel, ignoreCase = true) }?.second ?: 0.0
            val fertVal = currentFactors["Fertilizer Used"]?.find { it.first.equals(fertSel, ignoreCase = true) }?.second ?: 0.0

            val selectedFactors = mapOf(
                "Soil Type" to soilVal,
                "Irrigation Level" to irrVal,
                "Planting Density" to denVal,
                "Fertilizer Used" to fertVal
            )

            val adjustedYieldPerM2 = controller.calculateAdjustedYield(baseYield, selectedFactors)
            val adjustedYield = adjustedYieldPerM2 * area

            if (userId != null) {

                controller.saveCropData(
                    userId,
                    cropName,
                    area,
                    adjustedYield,
                    soilSel,
                    irrSel,
                    denSel,
                    fertSel,
                    selectedDateMillis
                )
                (requireActivity() as? MainActivity)?.controller?.switchFragment(R.id.nav_growth)
                inputCrop.setText("", false)
                inputArea.setText("")
                inputSoilType.setText("", false)
                inputIrrigation.setText("", false)
                inputPlantDensity.setText("", false)
                inputFertilizer.setText("", false)
                inputWeather.setText("")
                inputRegion.setText("")
            }
        }
        return view
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
        for (day in 1..daysInMonth) {
            val btnDay = Button(context)
            btnDay.text = day.toString()
            btnDay.textSize = 14f
            btnDay.setBackgroundResource(R.drawable.day_button_selector)
            btnDay.setTextColor(ContextCompat.getColorStateList(context, R.color.day_text_color))
            btnDay.layoutParams = GridLayout.LayoutParams().apply {
                width = 110
                height = 110
                setMargins(8, 8, 8, 8)
            }
            btnDay.setOnClickListener {
                for (i in 0 until calendarGrid.childCount) calendarGrid.getChildAt(i).isSelected = false
                btnDay.isSelected = true
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                this@CalcFragment.selectedDateMillis = selectedCalendar.timeInMillis
            }
            calendarGrid.addView(btnDay)
        }
    }
    private fun getCurrentUserId(): Int? {
        val session = com.example.farmforward.session.SessionManager(requireContext())
        return session.getUserId()
    }
}