package com.example.farmforward.fragmentController

import android.content.Context
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

var selectedDateMillis: Long = System.currentTimeMillis()

class CalcController(private val context: Context, private val cropViewModel: CropViewModel) {

    fun loadCropNames(): List<String> {
        val cropNames = mutableListOf<String>()
        try {
            val inputStream = context.assets.open("crop.csv")
            val reader = BufferedReader(inputStream.reader())
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.isNotEmpty()) {
                    val name = parts[0].trim()
                    if (name.isNotEmpty() && !cropNames.contains(name)) cropNames.add(name)
                }
            }
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Error loading CSV: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return cropNames
    }

    fun loadCropFactors(cropName: String): Map<String, List<Pair<String, Double>>> {
        val factors = mutableMapOf<String, MutableList<Pair<String, Double>>>()

        try {
            val inputStream = context.assets.open("crop_factors.csv")
            val reader = inputStream.bufferedReader()

            reader.readLine()

            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine // skip blank rows

                val parts = line.split(",")

                if (parts.size >= 4 && parts[0].trim().equals(cropName.trim(), ignoreCase = true)) {
                    val factor = parts[1].trim()       // e.g. "Soil Type"
                    val category = parts[2].trim()     // e.g. "Fertile"
                    val effect = parts[3].trim().toDoubleOrNull() ?: 0.0 // e.g. 10.0

                    factors.getOrPut(factor) { mutableListOf() }.add(category to effect)

                    android.util.Log.d("CROP_FACTORS", "Match: $cropName | $factor | $category | $effect")
                } else {
                    android.util.Log.d("CROP_FACTORS", "Skip: ${parts.getOrNull(0)} (looking for $cropName)")
                }
            }

            reader.close()

            android.util.Log.d("CROP_FACTORS", "Loaded factors for $cropName: ${factors.keys}")

            Toast.makeText(context, "Loaded ${factors.size} factor groups for $cropName", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error reading factors: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("CROP_FACTORS", "Error reading factors: ${e.message}")
        }


        return factors
    }


    fun calculateAdjustedYield(baseYield: Double, selectedEffects: Map<String, Double>): Double {
        var totalPercent = 0.0
        for (value in selectedEffects.values) {
            totalPercent += value
        }

        val adjusted = baseYield * (1 + totalPercent / 100)
        android.util.Log.d("YIELD_CALC", "Base: $baseYield | +$totalPercent% | Adjusted: $adjusted")
        return adjusted
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
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        for (day in 1..daysInMonth) {
            val btnDay = Button(context)
            btnDay.text = day.toString()
            btnDay.textSize = 14f
            btnDay.setBackgroundResource(R.drawable.day_button_selector)
            btnDay.setTextColor(ContextCompat.getColorStateList(context, R.drawable.day_text_color))
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
                selectedDateMillis = selectedCalendar.timeInMillis
            }
            calendarGrid.addView(btnDay)
        }
    }

    fun getYield(cropName: String): Double? {
        try {
            val inputStream = context.assets.open("crop.csv")
            val reader = inputStream.bufferedReader()
            reader.readLine()
            for (line in reader.lineSequence()) {
                val parts = line.split(",")
                if (parts.isNotEmpty() && parts[0].trim().equals(cropName.trim(), ignoreCase = true)) {
                    val yieldKgHa = parts.getOrNull(1)?.toDoubleOrNull()
                    if (yieldKgHa != null) {
                        val perSquareMeter = yieldKgHa / 10_000.0
                        return String.format("%.2f", perSquareMeter).toDouble()
                    }
                }
            }
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Error reading base yield: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return null
    }

    fun getHarvestDays(cropName: String): Pair<Int?, Int?> {
        try {
            val inputStream = context.assets.open("crop.csv")
            val reader = inputStream.bufferedReader()
            reader.readLine()
            for (line in reader.lineSequence()) {
                val parts = line.split(",")
                if (parts.isNotEmpty() && parts[0].trim().equals(cropName.trim(), ignoreCase = true)) {
                    val minDays = parts.getOrNull(2)?.trim()?.toIntOrNull()
                    val maxDays = parts.getOrNull(3)?.trim()?.toIntOrNull()
                    reader.close()
                    return Pair(minDays, maxDays)
                }
            }
            reader.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Error reading harvest days: ${e.message}", Toast.LENGTH_LONG).show()
        }
        return Pair(null, null)
    }

    fun saveCropData(userId: Int,
                     cropName: String,
                     area: Double,
                     expectedYield: Double,
                     soilType: String?,
                     irrigationLevel: String?,
                     plantDensity: String?,
                     fertilizerUsed: String?)
    {
        val (minDays, maxDays) = getHarvestDays(cropName)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDateMillis
        val minDateMillis = if (minDays != null) {
            calendar.add(Calendar.DAY_OF_YEAR, minDays)
            calendar.timeInMillis
        } else null
        calendar.timeInMillis = selectedDateMillis
        val maxDateMillis = if (maxDays != null) {
            calendar.add(Calendar.DAY_OF_YEAR, maxDays)
            calendar.timeInMillis
        } else null
        cropViewModel.setCropData(
            userId,
            cropName,
            area,
            expectedYield,
            selectedDateMillis,
            minDateMillis,
            maxDateMillis,
            soilType,
            irrigationLevel,
            plantDensity,
            fertilizerUsed
        )
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val minDateText = minDateMillis?.let { dateFormat.format(it) } ?: "N/A"
        val maxDateText = maxDateMillis?.let { dateFormat.format(it) } ?: "N/A"
        Toast.makeText(context, "✅ $cropName saved!\nHarvest: $minDateText - $maxDateText", Toast.LENGTH_LONG).show()
    }
}
