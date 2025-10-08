package com.jcotters.auth.data

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

class PasswordUtils @Inject constructor() {

  private companion object {
    const val SALT_SIZE_BYTES = 16
    const val HASHING_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val ITERATION_COUNT = 10000
    const val KEY_LENGTH_BITS = 256
  }

  fun generateSalt(): ByteArray {
    val salt = ByteArray(SALT_SIZE_BYTES)
    SecureRandom().nextBytes(salt)
    return salt
  }

  fun hashPassword(password: String, salt: ByteArray): String {
    val factory = SecretKeyFactory.getInstance(HASHING_ALGORITHM)
    val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
    return factory.generateSecret(spec).encoded.toHexString()
  }
}