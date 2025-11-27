package com.example.farmforward.database.roomDatabase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

    @Database(
        entities = [User::class, CropEntity::class],
        version = 8,
        exportSchema = false
    )
    abstract class AppDatabase : RoomDatabase() {
        abstract fun cropDao(): RoomCropDao
        abstract fun userDao(): RoomUserDao
        companion object {
            @Volatile
            private var INSTANCE: AppDatabase? = null

            fun getDatabase(context: Context): AppDatabase {
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "farmforward_database"
                    ).fallbackToDestructiveMigration().build()
                    INSTANCE = instance
                    instance
                }
            }
        }
    }
