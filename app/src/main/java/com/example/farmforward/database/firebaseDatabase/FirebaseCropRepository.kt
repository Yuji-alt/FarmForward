package com.example.farmforward.database.firebaseDatabase

import com.example.farmforward.database.roomDatabase.CropEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseCropRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private fun getUserCropsRef(userId: Int) =
        firestore.collection("users")
            .document(userId.toString())
            .collection("crops")

    suspend fun insertCrop(crop: CropEntity) {
        try {
            val docId = "${crop.cropName}_${crop.date}"

            val cropData = hashMapOf(
                "userId" to crop.userId,
                "cropName" to crop.cropName,
                "area" to crop.area,
                "expectedYield" to crop.expectedYield,
                "date" to crop.date,
                "maxdate" to crop.maxdate,
                "mindate" to crop.mindate,
                "soilType" to crop.soilType,
                "irrigationLevel" to crop.irrigationLevel,
                "plantDensity" to crop.plantDensity,
                "fertilizerUsed" to crop.fertilizerUsed,
                "lastUpdated" to crop.lastUpdated,
                "isSynced" to 1 // Mark as synced in the cloud
            )

            getUserCropsRef(crop.userId)
                .document(docId)
                .set(cropData, SetOptions.merge())
                .await()

        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    suspend fun getCropsForUser(userId: Int): List<CropEntity> {
        return try {
            val snapshot = getUserCropsRef(userId).get().await()
            snapshot.toObjects(CropEntity::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteCrop(userId: Int, cropName: String, date: Long) {
        try {
            val docId = "${cropName}_${date}"
            getUserCropsRef(userId).document(docId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}