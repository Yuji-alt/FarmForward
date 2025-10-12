package com.example.farmforward.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
@Dao
interface UserDao {

    @Insert
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM user_table WHERE username = :username AND password = :password")
    suspend fun loginUser(username: String, password: String): User?

    @Query("SELECT * FROM user_table WHERE username = :username")
    suspend fun checkUserExists(username: String): User?
}