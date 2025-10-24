package com.example.farmforward.fragmentController

import android.content.Context
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalcController(private val context: Context, private val cropViewModel: CropViewModel) {

    fun loadCropNames(): List<String> {
        val cropNames = mutableListOf<String>()
        try {
            val inputStream = context.assets.open("crop.csv")
            val reader = BufferedReader(inputStream.reader())
            reader.readLine() // skip header
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.isNotEmpty()) cropNames.add(parts[0].trim())
            }
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return cropNames
    }

    fun updateCalendar(
        calendar: Calendar,
        calendarGrid: GridLayout,
        tvMonthYear: TextView,
        context: Context
    ) {
        calendarGrid.removeAllViews()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonthYear.text = monthFormat.format(calendar.time)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (day in 1..daysInMonth) {
            val btnDay = Button(context)
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


    fun getYield(cropName: String): Double? {
        val inputStream = context.assets.open("crop.csv")
        val reader = inputStream.bufferedReader()
        reader.readLine() // skip header
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

    fun saveCropData(userId: Int, cropName: String, area: Double) {
        val yieldPerM2 = getYield(cropName)
        if (yieldPerM2 != null) {
            val expectedYield = yieldPerM2 * area
            cropViewModel.setCropData(userId, cropName, area, expectedYield)
            Toast.makeText(context, "✅ Crop saved for user $userId", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "❌ Crop not found", Toast.LENGTH_SHORT).show()
        }
    }
}
