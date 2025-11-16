package com.example.farmforward.firebase

import android.content.Context
import android.util.Log
import com.example.farmforward.appActivity.userSession.session.SessionManager
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.roomDatabase.CropEntity
import com.example.farmforward.database.roomDatabase.User
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
        val session = SessionManager(context)
        val userId = session.getUserId() ?: return@withContext

        try {
            val snapshot = firebaseCropRepo.db.collection("crops")
                .whereEqualTo("userId", userId.toLong())
                .get()
                .await()
            val firebaseCrops = snapshot.toObjects(CropEntity::class.java)
            val localCrops = cropDao.getCropsForUserList(userId)
            val getCropKey: (CropEntity) -> String = { "${it.cropName}_${it.date}" }

            val firebaseMap = firebaseCrops.associateBy(getCropKey)
            val localMap = localCrops.associateBy(getCropKey)

            val allCropKeys = firebaseMap.keys union localMap.keys
            for (key in allCropKeys) {
                val firebaseItem = firebaseMap[key]
                val localItem = localMap[key]

                when {
                    firebaseItem != null && localItem == null -> {
                        cropDao.insertCrop(firebaseItem)
                        Log.d("SyncCrops", "PULLED remote crop: $key")
                    }

                    firebaseItem == null && localItem != null -> {
                        firebaseCropRepo.insertCrop(localItem)
                        Log.d("SyncCrops", "PUSHED local crop: $key")
                    }

                    firebaseItem != null && localItem != null -> {
                        if (firebaseItem.lastUpdated > localItem.lastUpdated) {
                            // Firebase is newer -> Pull
                            cropDao.insertCrop(firebaseItem)
                            Log.d("SyncCrops", "UPDATED local crop from remote: $key")
                        } else if (firebaseItem.lastUpdated < localItem.lastUpdated) {
                            // Local is newer -> Push
                            firebaseCropRepo.insertCrop(localItem)
                            Log.d("SyncCrops", "UPDATED remote crop from local: $key")
                        }
                    }
                }
            }
            Log.d("SyncCrops", "Crop sync complete for user $userId")

        } catch (e: Exception) {
            Log.e("SyncCrops", "Error syncing crops: ${e.message}")
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
