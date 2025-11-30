package com.example.farmforward.database.firebaseDatabase

import android.util.Log
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.roomDatabase.RoomCropDao
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.database.roomDatabase.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseSyncManager @Inject constructor(
    private val userDao: RoomUserDao,
    private val cropDao: RoomCropDao,
    private val firebaseUserRepo: FirebaseUserRepository,
    private val session: SessionManager,
    private val firestore: FirebaseFirestore
) {

    suspend fun syncUsers() = withContext(Dispatchers.IO) {
        try {
            val firebaseUsers = firestore.collection("users").get().await()
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
                            val mergedUser = firebaseItem.copy(
                                password = if (firebaseItem.password.isNotEmpty()) firebaseItem.password else localItem.password
                            )
                            userDao.registerUser(mergedUser)

                        } else if (firebaseItem.lastUpdated < localItem.lastUpdated) {
                            firebaseUserRepo.registerUser(localItem)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncUsers", "Error syncing users: ${e.message}")
        }
    }

    suspend fun syncCrops() = withContext(Dispatchers.IO) {
        val userId = session.getUserId() ?: return@withContext

        try {
            val snapshot = firestore.collection("users")
                .document(userId.toString())
                .collection("crops")
                .get()
                .await()

            val firebaseCrops = snapshot.toObjects(CropEntity::class.java)
            val localCrops = cropDao.getAllCropsIncludeDeleted(userId)
            val getCropKey: (CropEntity) -> String = { it.firestoreId }

            val firebaseMap = firebaseCrops.associateBy(getCropKey)
            val localMap = localCrops.associateBy(getCropKey)

            val allCropKeys = firebaseMap.keys union localMap.keys

            for (key in allCropKeys) {
                val firebaseItem = firebaseMap[key]
                val localItem = localMap[key]

                when {
                    firebaseItem != null && localItem == null -> {
                        val syncedItem = firebaseItem.copy(isSynced = 1)
                        cropDao.insertCrop(syncedItem)
                    }

                    firebaseItem == null && localItem != null -> {
                        uploadCropToFirebase(userId, localItem)
                    }

                    firebaseItem != null && localItem != null -> {
                        if (localItem.isDeleted == 1) {
                            uploadCropToFirebase(userId, localItem)
                        }
                        else if (firebaseItem.lastUpdated > localItem.lastUpdated) {
                            val syncedItem = firebaseItem.copy(isSynced = 1)
                            cropDao.insertCrop(syncedItem)
                        }
                        else if (firebaseItem.lastUpdated < localItem.lastUpdated) {
                            uploadCropToFirebase(userId, localItem)
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
    private suspend fun uploadCropToFirebase(userId: Int, crop: CropEntity) {
        try {
            if (crop.isDeleted == 1) {
                firestore.collection("users")
                    .document(userId.toString())
                    .collection("crops")
                    .document(crop.firestoreId)
                    .delete()
                    .await()
                cropDao.deleteCropById(crop.id)
                firestore.collection("...").document(crop.firestoreId).delete().await()
                Log.d("SyncCrops", "Deleted crop permanently: ${crop.cropName}")
            } else {
                val uploadData = crop.copy(isSynced = 1)
                firestore.collection("users")
                    .document(userId.toString())
                    .collection("crops")
                    .document(crop.firestoreId)
                    .set(uploadData)
                    .await()

                cropDao.markAsSynced(crop.id)
            }
        } catch (e: Exception) {
            Log.e("SyncCrops", "Failed to sync crop: ${e.message}")
        }
    }
}