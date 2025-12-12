package com.example.farmforward.appActivity.mainActivity.garden

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.roomDatabase.AppDatabase
import javax.inject.Inject

class GardenController @Inject constructor(
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private var view: GardenView? = null
    private var cropsObserver: Observer<List<CropEntity>>? = null
    private var userId: Int = -1

    private var allCrops: List<CropEntity> = emptyList()
    private var currentSearchQuery: String = ""

    fun bindView(view: GardenView) {
        this.view = view
    }

    fun setupObserver(lifecycleOwner: LifecycleOwner) {
        userId = sessionManager.getUserId() ?: -1
        if (userId == -1) return

        cropsObserver = Observer { crops ->
            allCrops = crops
            processCrops()
        }

        cropsObserver?.let { observer ->
            db.cropDao().getCropsForUser(userId).observe(lifecycleOwner, observer)
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentSearchQuery = query
        processCrops()
    }

    private fun processCrops() {
        val filteredList = if (currentSearchQuery.isEmpty()) {
            allCrops
        } else {
            allCrops.filter { it.cropName.contains(currentSearchQuery, ignoreCase = true) }
        }

        val today = System.currentTimeMillis()

        // 1. Harvested: Has a harvest date
        val harvestedCrops = filteredList.filter { it.harvestedDate != null }

        // 2. Active (Non-Harvested)
        val nonHarvested = filteredList.filter { it.harvestedDate == null }

        // 3. Split Active into "Ready" and "Growing"
        val readyCrops = nonHarvested.filter { crop ->
            val minHarvest = crop.mindate ?: Long.MAX_VALUE
            today >= minHarvest
        }

        val growingCrops = nonHarvested.filter { crop ->
            val minHarvest = crop.mindate ?: Long.MAX_VALUE
            today < minHarvest
        }

        // Send all lists to the view
        view?.updateCropLists(growingCrops, readyCrops, harvestedCrops)
    }

    fun onCropClicked(crop: CropEntity) {
        view?.selectCropForGrowth(crop)
        view?.navigateToGrowth()
    }

    fun onAddClicked() {
        view?.navigateToCalc()
    }

    fun onDestroy() {
        if (userId != -1 && cropsObserver != null) {
            db.cropDao().getCropsForUser(userId).removeObserver(cropsObserver!!)
        }
        view = null
    }
}