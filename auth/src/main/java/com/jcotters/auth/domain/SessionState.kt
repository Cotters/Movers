package com.jcotters.auth.domain

data class SessionState(
  val userId: Int? = null,
  val expiresAt: Long = 0L
)
