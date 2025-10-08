package com.jcotters.auth.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUseCaseShould {

  private val underTest = LoginUseCase()

  @Test
  fun `return error message when details are incorrect`() {
    val result = underTest.invoke("username", "bad_password")

    assertFalse(result.isSuccess)
  }

  @Test
  fun `return success when details are correct`() {
    val result = underTest.invoke("username", "password")

    assertTrue(result.isSuccess)
  }
}