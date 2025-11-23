package com.example.farmforward.appActivity.calc

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.farmforward.appActivity.userSession.session.SessionManager
import com.example.farmforward.viewModel.CropViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import javax.inject.Inject

class CalcController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionManager
) {
    private var view: CalcView? = null
    private var cropViewModel: CropViewModel? = null
    private var scope: LifecycleCoroutineScope? = null

    private var currentFactors: Map<String, List<Pair<String, Double>>> = emptyMap()
    private var baseYield: Double = 0.0

    fun bindView(view: CalcView, viewModel: CropViewModel, scope: LifecycleCoroutineScope) {
        this.view = view
        this.cropViewModel = viewModel
        this.scope = scope
    }

    suspend fun onViewCreated() {
        val cropList = loadCropNames()
        view?.setCropAdapter(cropList)

    }

    suspend fun onCropSelected(cropName: String) {
        currentFactors = loadCropFactors(cropName)
        baseYield = getYield(cropName) ?: 0.0

        val soilOptions = currentFactors["Soil Type"]?.map { it.first } ?: emptyList()
        val irrigationOptions = currentFactors["Irrigation Level"]?.map { it.first } ?: emptyList()
        val densityOptions = currentFactors["Planting Density"]?.map { it.first } ?: emptyList()
        val fertOptions = currentFactors["Fertilizer Used"]?.map { it.first } ?: emptyList()

        view?.setFactorAdapters(soilOptions, irrigationOptions, densityOptions, fertOptions)
        view?.clearFactorInputs()
        view?.showToast("Factors loaded for $cropName")
    }

    fun onCalculateClicked(
        cropName: String,
        areaStr: String,
        soilSel: String,
        irrSel: String,
        denSel: String,
        fertSel: String,
        selectedDateMillis: Long
    ) {
        scope?.launch {
            val area = areaStr.toDoubleOrNull() ?: 0.0
            val userId = session.getUserId()

            if (cropName.isEmpty() || area <= 0.0) {
                view?.showToast("Please enter valid crop and area.")
                return@launch
            }
            if (soilSel.isEmpty() || irrSel.isEmpty() || denSel.isEmpty() || fertSel.isEmpty()) {
                view?.showToast("Please select all factor options first.")
                return@launch
            }
            if (userId == null) {
                view?.showToast("Error: User not logged in.")
                return@launch
            }
            if (baseYield <= 0.0) {
                baseYield = getYield(cropName) ?: 0.0
                if (baseYield <= 0.0) {
                    view?.showToast("Error: Could not load base yield for $cropName.")
                    return@launch
                }
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

            val adjustedYieldPerM2 = calculateAdjustedYield(baseYield, selectedFactors)
            val adjustedYield = adjustedYieldPerM2 * area

            try {
                saveCropData(
                    userId, cropName, area, adjustedYield, soilSel, irrSel,
                    denSel, fertSel, selectedDateMillis
                )

                view?.clearAllInputs()
                view?.navigateToLoading()

            } catch (e: Exception) {
                view?.showToast("Error saving crop: ${e.message}", Toast.LENGTH_LONG)
            }
        }
    }

    private suspend fun loadCropNames(): List<String> = withContext(Dispatchers.IO) {
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
        } catch (e: Exception) { e.printStackTrace() }
        return@withContext cropNames
    }

    private suspend fun loadCropFactors(cropName: String): Map<String, List<Pair<String, Double>>> = withContext(Dispatchers.IO) {
        val factors = mutableMapOf<String, MutableList<Pair<String, Double>>>()
        try {
            val inputStream = context.assets.open("crop_factors.csv")
            val reader = inputStream.bufferedReader()
            reader.readLine()
            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = line.split(",")
                if (parts.size >= 4 && parts[0].trim().equals(cropName.trim(), ignoreCase = true)) {
                    val factor = parts[1].trim()
                    val category = parts[2].trim()
                    val effect = parts[3].trim().toDoubleOrNull() ?: 0.0
                    factors.getOrPut(factor) { mutableListOf() }.add(category to effect)
                }
            }
            reader.close()
        } catch (e: Exception) { Log.e("CROP_FACTORS", "Error reading factors: ${e.message}") }
        return@withContext factors
    }

    private fun calculateAdjustedYield(baseYield: Double, selectedEffects: Map<String, Double>): Double {
        var totalPercent = 0.0
        selectedEffects.values.forEach { totalPercent += it }
        val adjusted = baseYield * (1 + totalPercent / 100)
        Log.d("YIELD_CALC", "Base: $baseYield | +$totalPercent% | Adjusted: $adjusted")
        return adjusted
    }

    private suspend fun getYield(cropName: String): Double? = withContext(Dispatchers.IO) {
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
                        reader.close()
                        return@withContext String.format("%.2f", perSquareMeter).toDouble()
                    }
                }
            }
            reader.close()
        } catch (e: Exception) { Log.e("getYield", "Error reading base yield: ${e.message}") }
        return@withContext null
    }

    private suspend fun getHarvestDays(cropName: String): Pair<Int?, Int?> = withContext(Dispatchers.IO) {
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
                    return@withContext Pair(minDays, maxDays)
                }
            }
            reader.close()
        } catch (e: Exception) { Log.e("getHarvestDays", "Error reading harvest days: ${e.message}") }
        return@withContext Pair(null, null)
    }

    private suspend fun saveCropData(
        userId: Int, cropName: String, area: Double, expectedYield: Double,
        soilType: String?, irrigationLevel: String?, plantDensity: String?,
        fertilizerUsed: String?, dateToPlant: Long
    ) {
        val (minDays, maxDays) = getHarvestDays(cropName)
        val baseCalendar = Calendar.getInstance().apply { timeInMillis = dateToPlant }
        val minDateMillis = if (minDays != null) {
            (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, minDays) }.timeInMillis
        } else null
        val maxDateMillis = if (maxDays != null) {
            (baseCalendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, maxDays) }.timeInMillis
        } else null

        val roundedYield = (expectedYield * 100).roundToInt() / 100.0

        cropViewModel?.saveNewCrop(
            userId, cropName, area, roundedYield, dateToPlant,
            minDateMillis, maxDateMillis, soilType, irrigationLevel,
            plantDensity, fertilizerUsed
        )

        withContext(Dispatchers.Main) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val minDateText = minDateMillis?.let { dateFormat.format(it) } ?: "N/A"
            val maxDateText = maxDateMillis?.let { dateFormat.format(it) } ?: "N/A"
            view?.showToast("✅ $cropName saved!\nHarvest: $minDateText - $maxDateText", Toast.LENGTH_LONG)
        }
    }
}