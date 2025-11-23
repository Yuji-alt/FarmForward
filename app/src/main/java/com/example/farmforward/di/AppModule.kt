package com.example.farmforward.di

import android.content.Context
import com.example.farmforward.appActivity.userSession.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.roomDatabase.RoomCropDao
import com.example.farmforward.database.roomDatabase.RoomUserDao
import com.example.farmforward.utils.RetrofitClient
import com.example.farmforward.utils.WeatherApi
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    @Provides
    fun provideUserDao(db: AppDatabase): RoomUserDao {
        return db.userDao()
    }
    @Provides
    fun provideCropDao(db: AppDatabase): RoomCropDao {
        return db.cropDao()
    }
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideWeatherApi(): WeatherApi {
        return RetrofitClient.instance
    }
}