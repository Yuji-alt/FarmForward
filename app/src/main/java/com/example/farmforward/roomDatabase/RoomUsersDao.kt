package com.example.farmforward.roomDatabase

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE )
    suspend fun registerUser(user: User)

    @Query("SELECT COUNT(*) FROM user_table WHERE username = :username")
    suspend fun checkUserExists(username: String): Int

    @Query("SELECT * FROM user_table WHERE username = :username AND password = :password LIMIT 1")
    suspend fun loginUser(username: String, password: String): User?

    @Query("SELECT * FROM user_table WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?
    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>
}
