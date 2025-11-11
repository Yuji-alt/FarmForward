package com.example.farmforward.firebase

import android.content.Context
import android.util.Log
import com.example.farmforward.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.roomDatabase.CropEntity
import com.example.farmforward.roomDatabase.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseSyncManager(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val firebaseUserRepo = FirebaseUserRepository()
    private val firebaseCropRepo = FirebaseCropRepository()

    suspend fun syncUsers() = withContext(Dispatchers.IO) {
        val userDao = db.userDao()
        val firebaseUsers = firebaseUserRepo.db.collection("users").get().await()
            .toObjects(User::class.java)
        val localUsers = userDao.getAllUsers()

        val firebaseMap = firebaseUsers.associateBy { it.username }
        val localMap = localUsers.associateBy { it.username }

        val allUsernames = firebaseMap.keys union localMap.keys

        for (username in allUsernames) {
            val firebaseItem = firebaseMap[username]
            val localItem = localMap[username]

            when {
                firebaseItem == null && localItem != null -> {
                    firebaseUserRepo.registerUser(localItem)
                }
                firebaseItem != null && localItem == null -> {
                    userDao.registerUser(firebaseItem)
                }
                firebaseItem != null && localItem != null -> {
                    if (firebaseItem.lastUpdated > localItem.lastUpdated) {
                        userDao.registerUser(firebaseItem)
                    } else if (firebaseItem.lastUpdated < localItem.lastUpdated) {
                        firebaseUserRepo.registerUser(localItem)
                    }
                }
            }
        }
    }

    suspend fun syncCrops() = withContext(Dispatchers.IO) {
        val cropDao = db.cropDao()
        val session = com.example.farmforward.session.SessionManager(context)
        val userId = session.getUserId() ?: return@withContext

        try {
            val snapshot = firebaseCropRepo.db.collection("crops")
                .whereEqualTo("userId", userId.toLong())
                .get()
                .await()

            val firebaseCrops = snapshot.toObjects(CropEntity::class.java)

            // ✅ Step 1: Get all local crops for this user
            val localCrops = cropDao.getCropsForUserList(userId)

            // ✅ Step 2: Only insert if not already present (by name + date)
            firebaseCrops.forEach { crop ->
                val exists = localCrops.any {
                    it.cropName.equals(crop.cropName, ignoreCase = true) &&
                            it.date == crop.date
                }

                if (!exists) {
                    cropDao.insertCrop(crop)
                }
            }

            // ✅ Optional: Push new local-only crops to Firebase
            localCrops.forEach { localCrop ->
                val match = firebaseCrops.find {
                    it.cropName.equals(localCrop.cropName, ignoreCase = true) &&
                            it.date == localCrop.date
                }
                if (match == null || localCrop.lastUpdated > match.lastUpdated) {
                    firebaseCropRepo.insertCrop(localCrop)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    suspend fun pushLocalToFirebase() = withContext(Dispatchers.IO) {
        val users = db.userDao().getAllUsers()
        val crops = db.cropDao().getAllCrops()

        users.forEach { firebaseUserRepo.registerUser(it) }
        crops.forEach { firebaseCropRepo.insertCrop(it) }
    }
}
