package com.example.farmforward.appActivity.mainActivity.growth

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.roomDatabase.AppDatabase
import javax.inject.Inject

class GrowthController @Inject constructor(
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private var view: GrowthView? = null
    private var cropsObserver: Observer<List<CropEntity>>? = null
    private var userId: Int = -1
    private var allCrops: List<CropEntity> = emptyList()
    private var currentSearchQuery: String = ""

    fun bindView(view: GrowthView) {
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

        val activeCrops = filteredList.filter { it.harvestedDate == null }

        view?.displayCrops(activeCrops)
    }
    fun onCropClicked(crop: CropEntity) {
        view?.navigateToCropDetails(crop)
    }

    fun onDestroy() {
        if (userId != -1 && cropsObserver != null) {
            db.cropDao().getCropsForUser(userId).removeObserver(cropsObserver!!)
        }
        view = null
    }
}