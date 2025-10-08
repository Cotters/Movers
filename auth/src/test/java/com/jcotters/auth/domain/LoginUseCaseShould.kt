package com.jcotters.auth.domain

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginUseCaseShould {

  @MockK
  private lateinit var repository: IUserRepository

  private lateinit var underTest: LoginUseCase

  @Before
  fun setUp() {
    MockKAnnotations.init(this)
    underTest = LoginUseCase(repository = repository)
  }

  @Test
  fun `return error message when details are incorrect`() {
    coEvery { repository.login("username", "bad_password") } returns Result.failure(Throwable("Fail"))

    val result = runBlocking { underTest.invoke("username", "bad_password") }

    assertFalse(result.isSuccess)
  }

  @Test
  fun `return success when details are correct`() {
    coEvery { repository.login("username", "password") } returns Result.success(Unit)

    val result = runBlocking { underTest.invoke("username", "password") }

    assertTrue(result.isSuccess)
  }
}