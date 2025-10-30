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
                for (i in 0 until calendarGrid.childCount) {
                    calendarGrid.getChildAt(i).isSelected = false
                }
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
        val inputStream = context.assets.open("crop.csv")
        val reader = inputStream.bufferedReader()
        reader.readLine() // skip header
        for (line in reader.lineSequence()) {
            val parts = line.split(",")
            if (parts.isNotEmpty() && parts[0].equals(cropName, ignoreCase = true)) {
                val yieldKgHa = parts.getOrNull(1)?.toDoubleOrNull()
                if (yieldKgHa != null) {
                    val perSquareMeter = yieldKgHa / 10_000.0
                    return String.format("%.2f", perSquareMeter).toDouble()
                }
            }
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
                if (parts.isNotEmpty() && parts[0].equals(cropName, ignoreCase = true)) {
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


    fun saveCropData(userId: Int, cropName: String, area: Double) {
        val yieldPerM2 = getYield(cropName)
        val (minDays, maxDays) = getHarvestDays(cropName)

        if (yieldPerM2 != null) {
            val expectedYield = yieldPerM2 * area
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selectedDateMillis

            val minDateMillis = if (minDays != null) {
                calendar.add(Calendar.DAY_OF_YEAR, minDays)
                calendar.timeInMillis
            } else null

            calendar.timeInMillis = selectedDateMillis // reset
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
                maxDateMillis
            )

            // Format and show confirmation
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val minDateText = minDateMillis?.let { dateFormat.format(it) } ?: "N/A"
            val maxDateText = maxDateMillis?.let { dateFormat.format(it) } ?: "N/A"

            Toast.makeText(
                context,
                "✅ $cropName saved!\nHarvest: $minDateText - $maxDateText",
                Toast.LENGTH_LONG
            ).show()

        } else {
            Toast.makeText(context, "❌ Crop not found", Toast.LENGTH_SHORT).show()
        }
    }
}
