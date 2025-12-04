package com.example.farmforward.database.roomDatabase

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoomUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registerUser(user: User)

    @Query("SELECT COUNT(*) FROM user_table WHERE username = :username")
    suspend fun checkUserExists(username: String): Int

    @Query("SELECT * FROM user_table WHERE (username = :identifier OR email = :identifier) AND password = :password LIMIT 1")
    suspend fun loginUser(identifier: String, password: String): User?

    @Query("SELECT * FROM user_table WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM user_table WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM user_table")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM user_table WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Int): User?

    // Fixed table name and column name: 'name' -> 'username'
    @Query("UPDATE user_table SET username = :newName WHERE id = :id")
    suspend fun updateUsername(id: Int, newName: String)

    @Query("UPDATE user_table SET password = :newPassword WHERE id = :id")
    suspend fun updatePassword(id: Int, newPassword: String)

    @Delete
    suspend fun delete(user: User)
}