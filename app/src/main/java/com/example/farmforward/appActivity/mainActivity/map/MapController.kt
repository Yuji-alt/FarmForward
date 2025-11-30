package com.example.farmforward.appActivity.mainActivity.map

import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.roomDatabase.RoomCropDao
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng // ADD THIS IMPORT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MapController @Inject constructor(
    private val cropDao: RoomCropDao,
    private val session: SessionManager
) {

    private var view: MapView? = null
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun bindView(view: MapView) {
        this.view = view
    }
        fun onMapReady(focusTarget: LatLng? = null) {
        val userId = session.getUserId() ?: -1
        if (userId == -1) return

        ioScope.launch {
            val crops = cropDao.getCropsForUserList(userId)

            withContext(Dispatchers.Main) {
                view?.clearMarkers()

                var hasCrops = false
                var firstLat = 0.0
                var firstLng = 0.0

                for (crop in crops) {
                    if (crop.latitude != 0.0 && crop.longitude != 0.0) {
                        hasCrops = true
                        if (firstLat == 0.0) {
                            firstLat = crop.latitude
                            firstLng = crop.longitude
                        }

                        val hue = calculateMarkerColor(crop)
                        val statusText = calculateStatusText(crop)
                        view?.addMarker(crop, hue, statusText)
                    }
                }
                if (focusTarget != null) {
                    view?.moveCamera(focusTarget.latitude, focusTarget.longitude)
                } else if (hasCrops) {
                    view?.moveCamera(firstLat, firstLng)
                }
            }
        }
    }

    private fun calculateStatusText(crop: CropEntity): String {
        val today = System.currentTimeMillis()
        if (today < crop.date) {
            val diff = crop.date - today
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff) + 1
            return "Scheduled: Planting in $days days"
        }
        val minHarvest = crop.mindate ?: return "Yield: ${crop.expectedYield} kg"
        val diff = minHarvest - today
        val daysDiff = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            daysDiff < 0 -> "Overdue by ${kotlin.math.abs(daysDiff)} days"
            daysDiff == 0L -> "Harvest TODAY!"
            daysDiff < 7 -> "Harvest in $daysDiff days"
            else -> "Growing (${daysDiff} days left)"
        }
    }

    fun onMarkerClicked(crop: CropEntity) {
        view?.navigateToGrowth(crop)
    }

    private fun calculateMarkerColor(crop: CropEntity): Float {
        val today = System.currentTimeMillis()
        if (today < crop.date) return BitmapDescriptorFactory.HUE_AZURE
        val minHarvest = crop.mindate ?: return BitmapDescriptorFactory.HUE_GREEN
        val maxHarvest = crop.maxdate ?: return BitmapDescriptorFactory.HUE_GREEN
        if (today > maxHarvest) return BitmapDescriptorFactory.HUE_RED
        val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
        if (today >= (minHarvest - sevenDaysInMillis)) return BitmapDescriptorFactory.HUE_YELLOW
        return BitmapDescriptorFactory.HUE_GREEN
    }

    fun onDestroy() {
        ioScope.cancel()
        view = null
    }
}