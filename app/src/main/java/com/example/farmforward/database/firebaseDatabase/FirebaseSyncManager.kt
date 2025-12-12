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
    private val session: SessionManager,
    private val firestore: FirebaseFirestore
) {
    private val TAG = "FirebaseSyncManager"

    private fun getFirebaseUserDocId(): String? {
        return session.getUserName()?.lowercase()
    }

    suspend fun syncUsers() = withContext(Dispatchers.IO) {
        // ... (Keep existing syncUsers logic) ...
        try {
            val userDocId = session.getUserName()?.lowercase() ?: return@withContext
            val userDocRef = firestore.collection("users").document(userDocId)
            val documentSnapshot = userDocRef.get().await()

            if (documentSnapshot.exists()) {
                var remoteUser = documentSnapshot.toObject(User::class.java)
                if (remoteUser != null) {
                    val existingLocalUser = userDao.getUserByUsername(remoteUser.username)
                        ?: userDao.getUserByEmail(remoteUser.email)

                    if (existingLocalUser != null) {
                        remoteUser = remoteUser.copy(
                            id = existingLocalUser.id,
                            password = if (existingLocalUser.password.isNotEmpty()) existingLocalUser.password else remoteUser.password
                        )
                    } else {
                        remoteUser = remoteUser.copy(id = 0)
                    }
                    userDao.registerUser(remoteUser)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncUsers failed: ${e.message}")
        }
    }
    suspend fun syncCrops(explicitDocId: String? = null) = withContext(Dispatchers.IO) {
        val userDocId = explicitDocId ?: getFirebaseUserDocId()
        val userId = session.getUserId()

        if (userDocId.isNullOrBlank() || userId == null) {
            Log.e(TAG, "Missing user identifiers. Sync aborted.")
            return@withContext
        }

        try {
            // 1. PUSH LOCAL UNSYNCED CROPS
            val unsyncedLocalCrops = cropDao.getUnsyncedCrops(userId)
            for (crop in unsyncedLocalCrops) {
                uploadCropToFirebase(userDocId, crop)
            }

            // 2. PULL REMOTE CROPS
            val snapshot = firestore.collection("users")
                .document(userDocId)
                .collection("crops")
                .get()
                .await()

            val firebaseCrops = snapshot.toObjects(CropEntity::class.java)
            val localCrops = cropDao.getAllCropsIncludeDeleted(userId)

            val getKey: (CropEntity) -> String = { it.firestoreId }
            val firebaseMap = firebaseCrops.associateBy(getKey)
            val localMap = localCrops.associateBy(getKey)

            val allKeys = firebaseMap.keys union localMap.keys

            for (key in allKeys) {
                val firebaseItem = firebaseMap[key]
                val localItem = localMap[key]

                when {
                    firebaseItem != null && localItem == null -> {
                        if (firebaseItem.isDeleted == 0) {
                            cropDao.insertCrop(firebaseItem.copy(isSynced = 1, userId = userId))
                        }
                    }
                    firebaseItem != null && localItem != null -> {
                        if (localItem.isDeleted == 1) {
                            uploadCropToFirebase(userDocId, localItem)
                        } else if (firebaseItem.lastUpdated > localItem.lastUpdated) {
                            cropDao.insertCrop(firebaseItem.copy(id = localItem.id, isSynced = 1, userId = userId))
                        } else if (firebaseItem.lastUpdated < localItem.lastUpdated) {
                            uploadCropToFirebase(userDocId, localItem)
                        }
                    }
                    firebaseItem == null && localItem != null -> {
                        if (localItem.isSynced == 1) {
                            cropDao.deleteCropById(localItem.id)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncCrops", "Error syncing crops: ${e.message}")
        }
    }

    private suspend fun uploadCropToFirebase(userDocId: String, crop: CropEntity) {
        try {
            val userCropsRef = firestore.collection("users")
                .document(userDocId)
                .collection("crops")

            val docRef = if (crop.firestoreId.isBlank()) {
                userCropsRef.document()
            } else {
                userCropsRef.document(crop.firestoreId)
            }

            if (crop.isDeleted == 1) {
                docRef.delete().await()
                cropDao.deleteCropById(crop.id)
            } else {
                val newId = docRef.id
                val uploadData = crop.copy(
                    firestoreId = newId,
                    isSynced = 1,
                    lastUpdated = System.currentTimeMillis()
                )
                docRef.set(uploadData).await()
                cropDao.markAsSynced(crop.id, newId)
            }
        } catch (e: Exception) {
            Log.e("SyncCrops", "Failed to sync crop: ${e.message}")
            throw e // Rethrow to notify caller (SettingsFragment)
        }
    }
}