package com.example.farmforward.appActivity.mainActivity.map

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.example.farmforward.R
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.viewModel.CropViewModel
import javax.inject.Inject

class MapController @Inject constructor(
    private val db: AppDatabase,
    private val sessionManager: SessionManager
) {
    private var view: MapView? = null
    private var cropsObserver: Observer<List<CropEntity>>? = null
    private var userId: Int = -1

    private var allCrops: List<CropEntity> = emptyList()
    private var currentSearchQuery: String = ""

    fun bindView(view: MapView) {
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
    fun forceRefreshCrops() {
        processCrops()
    }

    private fun processCrops() {
        val filteredList = if (currentSearchQuery.isEmpty()) {
            allCrops
        } else {
            allCrops.filter { it.cropName.contains(currentSearchQuery, ignoreCase = true) }
        }

        view?.displayCropsOnMap(filteredList)
    }

    fun onCropMarkerClicked(crop: CropEntity, viewModel: CropViewModel) {
        viewModel.viewCropDetails(crop)
        viewModel.lastSourceId = R.id.nav_garden

        view?.navigateToCropDetails()
    }

    fun onDestroy() {
        if (userId != -1 && cropsObserver != null) {
            db.cropDao().getCropsForUser(userId).removeObserver(cropsObserver!!)
        }
        view = null
    }
}