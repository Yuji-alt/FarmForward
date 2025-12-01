package com.example.farmforward.appActivity.mainActivity.garden

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.CropEntity
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
        // 1. Filter by Search Query first
        val filteredList = if (currentSearchQuery.isEmpty()) {
            allCrops
        } else {
            allCrops.filter { it.cropName.contains(currentSearchQuery, ignoreCase = true) }
        }

        val today = System.currentTimeMillis()

        val activeCrops = filteredList.filter { it.harvestedDate == null }
        val harvestedCrops = filteredList.filter { it.harvestedDate != null }
        val activeCount = activeCrops.size

        val readyToHarvestCount = activeCrops.count { crop ->
            val minHarvest = crop.mindate ?: Long.MAX_VALUE
            today >= minHarvest
        }

        view?.updateDashboardCounts(activeCount, readyToHarvestCount)
        view?.displayActiveCrops(activeCrops)
        view?.displayHarvestedCrops(harvestedCrops)
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