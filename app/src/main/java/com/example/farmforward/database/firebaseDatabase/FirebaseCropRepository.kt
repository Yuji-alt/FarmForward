package com.example.farmforward.firebase

import com.example.farmforward.database.roomDatabase.CropEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseCropRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    private val cropsRef = firestore.collection("crops")

    suspend fun insertCrop(crop: CropEntity) {
        val cropData = hashMapOf(
            "userId" to crop.userId.toLong(),
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
            "lastUpdated" to crop.lastUpdated
        )
        val docId = "${crop.userId}_${crop.cropName}_${crop.date}"
        cropsRef.document(docId).set(cropData).await()
    }

    suspend fun getCropsForUser(userId: Int): List<CropEntity> {
        val snapshot = cropsRef.whereEqualTo("userId", userId).get().await()
        return snapshot.toObjects(CropEntity::class.java)
    }
}