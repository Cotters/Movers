package com.jcotters.database.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertUser(user: User): Long

  @Query("SELECT * FROM users WHERE username like :username")
  suspend fun findByUsername(username: String): User?

  @Query("SELECT * FROM users WHERE id = :userId")
  suspend fun getUserById(userId: Int): User?

  @Query("SELECT COUNT(*) FROM users")
  suspend fun getUserCount(): Int

  @Delete
  suspend fun delete(user: User)
}