package com.example.farmforward.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.database.CropViewModel
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.*

class CalcFragment : Fragment() {

    private lateinit var calendar: Calendar
    private lateinit var tvMonthYear: TextView
    private lateinit var calendarGrid: GridLayout
    private lateinit var cropViewModel: CropViewModel
    private lateinit var inputCrop: AutoCompleteTextView
    private lateinit var inputArea: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calc, container, false)

        // ✅ Setup
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        inputCrop = view.findViewById(R.id.inputCrop)
        inputArea = view.findViewById(R.id.inputArea)
        val btnCalculate = view.findViewById<Button>(R.id.btnCalculate)

        // ✅ Calendar setup
        calendar = Calendar.getInstance()
        tvMonthYear = view.findViewById(R.id.tvMonthYear)
        calendarGrid = view.findViewById(R.id.calendarGrid)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNextMonth)

        updateCalendar()
        btnPrev.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateCalendar()
        }
        btnNext.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        inputCrop.setOnClickListener {
            inputCrop.showDropDown()
        }

        // ✅ Load crops into dropdown
        val cropList = loadCropNamesFromCSV()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cropList)
        inputCrop.setAdapter(adapter)
        inputCrop.threshold = 1

        // ✅ Calculate yield
        btnCalculate.setOnClickListener {
            val cropName = inputCrop.text.toString().trim()
            val area = inputArea.text.toString().toDoubleOrNull() ?: 0.0

            if (cropName.isNotEmpty()) {
                val yieldPerM2 = getYieldFromCSV(cropName)
                if (yieldPerM2 != null) {
                    val expectedYield = yieldPerM2 * area
                    cropViewModel.setCropData(cropName, area, expectedYield)
                    Toast.makeText(requireContext(), "✅ Crop selected: $cropName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "❌ Crop not found in CSV", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please select a crop", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // ✅ Calendar updater
    private fun updateCalendar() {
        calendarGrid.removeAllViews()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthFormat.format(calendar.time)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val btnDay = Button(requireContext())
            btnDay.text = day.toString()
            btnDay.textSize = 14f
            btnDay.setBackgroundResource(R.drawable.day_button_selector)
            btnDay.layoutParams = GridLayout.LayoutParams().apply {
                width = 110
                height = 110
                setMargins(8, 8, 8, 8)
            }
            btnDay.setOnClickListener {
                for (i in 0 until calendarGrid.childCount) {
                    calendarGrid.getChildAt(i).isSelected = false
                }
                btnDay.isSelected = true
            }
            calendarGrid.addView(btnDay)
        }
    }

    private fun loadCropNamesFromCSV(): List<String> {
        val cropNames = mutableListOf<String>()
        try {
            val inputStream = resources.openRawResource(R.raw.crop)
            val reader = BufferedReader(inputStream.reader())
            reader.readLine() // skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.isNotEmpty()) cropNames.add(parts[0].trim())
            }
            reader.close()
            Toast.makeText(requireContext(), "Loaded ${cropNames.size} crops", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return cropNames
    }

    private fun getYieldFromCSV(cropName: String): Double? {
        val inputStream = resources.openRawResource(R.raw.crop)
        val reader = inputStream.bufferedReader()
        reader.readLine()
        for (line in reader.lineSequence()) {
            val parts = line.split(",")
            if (parts.isNotEmpty() && parts[0].equals(cropName, ignoreCase = true)) {
                val yieldKgHa = parts.getOrNull(1)?.toDoubleOrNull()
                if (yieldKgHa != null) {
                    return yieldKgHa / 10_000.0
                }
            }
        }
        return null
    }
}
