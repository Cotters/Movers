package com.jcotters.auth.domain

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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
    coEvery { repository.login(USERNAME, "bad_password") } returns Result.failure(Throwable("Fail"))

    val result = runBlocking { underTest.invoke(USERNAME, "bad_password") }

    assertFalse(result.isSuccess)
    coVerify(exactly = 0) { repository.createUserSession(USER_ID) }
  }

  @Test
  fun `return success when details are correct`() {
    coEvery { repository.login(USERNAME, "password") } returns Result.success(MOVER_USER)
    coEvery { repository.createUserSession(USER_ID) } returns Result.success(Unit)

    val result = runBlocking { underTest.invoke(USERNAME, "password") }

    coVerify(exactly = 1) { repository.createUserSession(USER_ID) }
    assertTrue(result.isSuccess)
  }

  @Test
  fun `return failure when unable to create user session`() {
    coEvery { repository.login(USERNAME, "password") } returns Result.success(MOVER_USER)
    coEvery { repository.createUserSession(USER_ID) } returns Result.failure(Throwable("Test"))

    val result = runBlocking { underTest.invoke(USERNAME, "password") }

    coVerify(exactly = 1) { repository.createUserSession(USER_ID) }
    assertTrue(result.isFailure)
  }

  private companion object {
    const val USER_ID = 1
    const val USERNAME = "username"
    val MOVER_USER = MoverUser(userId = 1, username = USERNAME)
  }
}