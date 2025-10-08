package com.jcotters.database.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.random.Random

@Entity(tableName = "users")
data class User(
  @PrimaryKey(autoGenerate = true)
  val id: Int = Random.nextInt(),
  val username: String,
)