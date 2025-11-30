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

    fun bindView(view: GardenView) {
        this.view = view
    }

    fun setupObserver(lifecycleOwner: LifecycleOwner) {
        userId = sessionManager.getUserId() ?: -1

        if (userId == -1) return

        cropsObserver = Observer { crops ->
            view?.displayCrops(crops)
        }

        cropsObserver?.let { observer ->
            db.cropDao().getCropsForUser(userId).observe(lifecycleOwner, observer)
        }
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