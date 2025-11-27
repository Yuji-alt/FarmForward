package com.example.farmforward.appActivity.mainActivity.calc

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.staticData.CropRepository
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.example.farmforward.database.viewModel.CropViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToInt

class CalcController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cropRepository: CropRepository,
    private val sessionManager: SessionManager
) {

    private var view: CalcView? = null
    private var viewModel: CropViewModel? = null
    private var scope: LifecycleCoroutineScope? = null

    fun bindView(view: CalcView, viewModel: CropViewModel, scope: LifecycleCoroutineScope) {
        this.view = view
        this.viewModel = viewModel
        this.scope = scope
    }

    fun onViewCreated() {
        cropRepository.loadData()
        val cropNames = cropRepository.getAllCropNames()

        view?.setCropAdapter(cropNames)
        view?.setFactorAdapters(
            soilOptions = listOf("Poor", "Medium", "Fertile"),
            irrigationOptions = listOf("Rainfed", "Supplementary", "Fully irrigated"),
            densityOptions = listOf("Low", "Optimal", "Overcrowded"),
            fertOptions = listOf("None/Low", "Recommended NPK", "Balanced + Organic")
        )
        onFragmentVisible()
    }
    fun onFragmentVisible() {
        if (viewModel?.formDraft != null) {
            if (viewModel?.cropToEdit != null) {
                view?.setButtonText("Update Calculation")
            } else {
                view?.setButtonText("Calculate & Save")
            }
            return
        }

        if (viewModel?.cropToEdit != null) {
            val crop = viewModel!!.cropToEdit!!
            view?.preFillForm(crop)
            view?.setButtonText("Update Calculation")
        }
        else {
            view?.setButtonText("Calculate & Save")
        }
    }

    fun onCancelClicked() {
        view?.clearAllInputs()

        viewModel?.cropToEdit = null
        viewModel?.clearPickedLocation()

        view?.setButtonText("Calculate & Save")

        view?.showToast("Calculation cleared/cancelled.", isError = false)
    }

    fun onCropSelected(cropName: String) {
        if (cropName.isEmpty()) return

        val cropData = cropRepository.getCropData(cropName)

        if (cropData != null) {
            val message = "${cropData.category}: Harvests in ${cropData.minDays}-${cropData.maxDays} days"
            view?.showToast(message, isError = false)
            view?.clearFactorInputs()
        } else {
            view?.showToast("Error loading data for $cropName", isError = true)
        }
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
        if (cropName.isEmpty() || areaStr.isEmpty() ||
            soilSel.isEmpty() || irrSel.isEmpty() ||
            denSel.isEmpty() || fertSel.isEmpty()
        ) {
            view?.showToast("Please fill all fields.", isError = true)
            return
        }
        val cropData = cropRepository.getCropData(cropName)
        if (cropData == null) {
            view?.showToast("Crop data not found.", isError = true)
            return
        }
        val area = areaStr.toDoubleOrNull() ?: 0.0
        val category = cropData.category
        val soilEffect = cropRepository.getFactorEffect(category, "Soil Type", soilSel)
        val irrigationEffect = cropRepository.getFactorEffect(category, "Irrigation Level", irrSel)
        val densityEffect = cropRepository.getFactorEffect(category, "Planting Density", denSel)
        val fertilizerEffect = cropRepository.getFactorEffect(category, "Fertilizer Used", fertSel)
        val weatherEffect = 0.0
        val baseYieldPerSqm = cropData.baseYield / 10000.0
        val totalEffectPercent =
            soilEffect + irrigationEffect + densityEffect + fertilizerEffect + weatherEffect
        val adjustedYieldPerSqm = baseYieldPerSqm * (1 + (totalEffectPercent / 100.0))
        val totalYield = adjustedYieldPerSqm * area
        val roundedYield = (totalYield * 100.0).roundToInt() / 100.0
        val plantedDate = Date(selectedDateMillis)
        val cal = Calendar.getInstance()
        cal.time = plantedDate
        cal.add(Calendar.DAY_OF_YEAR, cropData.minDays)
        val minHarvestDate = cal.time

        cal.time = plantedDate
        cal.add(Calendar.DAY_OF_YEAR, cropData.maxDays)
        val maxHarvestDate = cal.time

        val userId = sessionManager.getUserId() ?: -1
        if (userId == -1) {
            view?.showToast("Error: User session not found.", isError = false)
            return
        }
        val isOnline = NetworkUtils.isNetworkAvailable(context)
        val syncStatus = if (isOnline) 1 else 0
        view?.getCurrentLocation { lat, lng ->
            scope?.launch {
                if (viewModel?.cropToEdit != null) {
                    viewModel?.updateCrop(
                        originalCrop = viewModel!!.cropToEdit!!,
                        area = area,
                        roundedYield = roundedYield,
                        dateToPlant = plantedDate.time,
                        minDateMillis = minHarvestDate.time,
                        maxDateMillis = maxHarvestDate.time,
                        soilType = soilSel,
                        irrigationLevel = irrSel,
                        plantDensity = denSel,
                        fertilizerUsed = fertSel,
                        isSynced = syncStatus,
                        latitude = lat,
                        longitude = lng
                    )
                    viewModel?.cropToEdit = null

                } else {
                    viewModel?.saveNewCrop(
                        userId = userId,
                        cropName = cropName,
                        area = area,
                        roundedYield = roundedYield,
                        dateToPlant = plantedDate.time,
                        minDateMillis = minHarvestDate.time,
                        maxDateMillis = maxHarvestDate.time,
                        soilType = soilSel,
                        irrigationLevel = irrSel,
                        plantDensity = denSel,
                        fertilizerUsed = fertSel,
                        isSynced = syncStatus,
                        latitude = lat,
                        longitude = lng
                    )
                }
                view?.clearAllInputs()
                view?.navigateToLoading(isOnline)
            }
        }
    }
}