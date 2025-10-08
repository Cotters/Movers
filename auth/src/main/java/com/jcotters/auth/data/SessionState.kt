package com.jcotters.auth.data

data class SessionState(
  val userId: Int? = null,
  val expiresAt: Long = 0L
)