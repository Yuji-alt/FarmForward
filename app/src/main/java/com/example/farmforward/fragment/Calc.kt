package com.example.farmforward.fragment

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
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import com.example.farmforward.activityViewmodel.MainActivity
import com.example.farmforward.fragmentController.CalcController
import com.example.farmforward.fragmentController.selectedDateMillis
import java.util.Calendar

class CalcFragment : Fragment() {

    private lateinit var controller: CalcController
    private lateinit var cropViewModel: CropViewModel
    private lateinit var inputCrop: AutoCompleteTextView
    private lateinit var inputArea: EditText
    private var currentFactors: Map<String, List<Pair<String, Double>>> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calc, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        controller = CalcController(requireContext(), cropViewModel)

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

        // ✅ Load crop list
        val cropList = controller.loadCropNames()
        val adapter = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, cropList)
        inputCrop.setAdapter(adapter)

        // ✅ When crop is chosen → load factors and setup dropdowns
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

            // ✅ Dropdown shows on first tap OR focus
            val dropdowns = listOf(inputSoilType, inputIrrigation, inputPlantDensity, inputFertilizer)
            dropdowns.forEach { input ->
                input.setOnClickListener { input.showDropDown() }
                input.setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) input.showDropDown()
                }
            }

            inputSoilType.postDelayed({ inputSoilType.showDropDown() }, 150)



            Toast.makeText(requireContext(), "Factors loaded for $cropName", Toast.LENGTH_SHORT).show()
        }

        // ✅ Calendar navigation
        val calendar = Calendar.getInstance()
        controller.updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            controller.updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }
        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            controller.updateCalendar(calendar, calendarGrid, tvMonthYear, requireContext())
        }

        // ✅ Calculation logic with validation
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
                    fertSel
                )

                // ✅ Restore navigation to GrowthFragment (Garden)
                val (minDays, maxDays) = controller.getHarvestDays(cropName)
                val selectedDate = selectedDateMillis
                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }

                val minHarvest = minDays?.let { cal.clone() as Calendar }.apply { this?.add(Calendar.DAY_OF_YEAR, minDays ?: 0) }?.timeInMillis
                val maxHarvest = maxDays?.let { cal.clone() as Calendar }.apply { this?.add(Calendar.DAY_OF_YEAR, maxDays ?: 0) }?.timeInMillis

                val growthFragment = GrowthFragment().apply {
                    arguments = Bundle().apply {
                        putString("cropName", cropName)
                        putDouble("area", area)
                        putDouble("expectedYield", adjustedYield)
                        putLong("datePlanted", selectedDate)
                        putLong("minHarvestDate", minHarvest ?: 0L)
                        putLong("maxHarvestDate", maxHarvest ?: 0L)
                        putString("soilType", soilSel)
                        putString("irrigationLevel", irrSel)
                        putString("plantDensity", denSel)
                        putString("fertilizerUsed", fertSel)
                    }
                }


                parentFragmentManager.beginTransaction()
                    .hide(this@CalcFragment)
                    .add(R.id.fragment_container, growthFragment)
                    .addToBackStack(null)
                    .commit()

                (requireActivity() as? MainActivity)?.controller?.setActiveMenu(R.id.nav_growth)
            }
        }



        return view
    }

    private fun getCurrentUserId(): Int? {
        val session = com.example.farmforward.session.SessionManager(requireContext())
        return session.getUserId()
    }
}