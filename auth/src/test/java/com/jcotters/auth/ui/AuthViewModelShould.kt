package com.jcotters.auth.ui

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialException
import com.jcotters.auth.domain.IUserRepository
import com.jcotters.auth.domain.LoginUseCase
import com.jcotters.auth.domain.SignUpUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class AuthViewModelShould {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var loginUseCase: LoginUseCase

    @MockK
    private lateinit var signUpUseCase: SignUpUseCase

    @MockK
    private lateinit var userRepository: IUserRepository

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var credentialManager: CredentialManager

    private lateinit var underTest: AuthViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        underTest = AuthViewModel(
            loginUseCase = loginUseCase,
            signUpUseCase = signUpUseCase,
            userRepository = userRepository,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `have correct initial ui state`() {
        val state = underTest.uiState.value

        assertThat(state.authMode, equalTo(AuthMode.Login))
        assertThat(state.username, equalTo(""))
        assertThat(state.password, equalTo(""))
        assertThat(state.confirmPassword, equalTo(""))
        assertFalse(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(""))
        assertThat(state.userCount, equalTo(0))
        assertFalse(state.loginButtonEnabled)
        assertFalse(state.signUpButtonEnabled)
    }

    @Test
    fun `update username and clear error message on UsernameUpdated event`() {
        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))

        val state = underTest.uiState.value
        assertThat(state.username, equalTo(TEST_USERNAME))
        assertThat(state.errorMessage, equalTo(""))
        assertTrue(state.loginButtonEnabled.not())
    }

    @Test
    fun `update password and clear error message on PasswordUpdated event`() {
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))

        val state = underTest.uiState.value
        assertThat(state.password, equalTo(TEST_PASSWORD))
        assertThat(state.errorMessage, equalTo(""))
    }

    @Test
    fun `update confirm password and clear error message on ConfirmPasswordUpdated event`() {
        underTest.onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(TEST_PASSWORD))

        val state = underTest.uiState.value
        assertThat(state.confirmPassword, equalTo(TEST_PASSWORD))
        assertThat(state.errorMessage, equalTo(""))
    }

    @Test
    fun `switch to SignUp mode, clear error message, and fetch user count on CreateAccountTapped event`() {
        coEvery { userRepository.getUserCount() } returns EXPECTED_USER_COUNT

        underTest.onViewEvent(AuthViewEvent.CreateAccountTapped)

        val state = underTest.uiState.value
        assertThat(state.authMode, equalTo(AuthMode.SignUp))
        assertThat(state.errorMessage, equalTo(""))
        assertThat(state.userCount, equalTo(EXPECTED_USER_COUNT))
        coVerify(exactly = 1) { userRepository.getUserCount() }
    }

    @Test
    fun `reset user count to 0 when fetching user count fails on CreateAccountTapped event`() {
        coEvery { userRepository.getUserCount() } throws RuntimeException("Database error")

        underTest.onViewEvent(AuthViewEvent.CreateAccountTapped)

        val state = underTest.uiState.value
        assertThat(state.authMode, equalTo(AuthMode.SignUp))
        assertThat(state.userCount, equalTo(0))
    }

    @Test
    fun `retain the last user count when refreshing it is cancelled`() {
        coEvery { userRepository.getUserCount() } returns EXPECTED_USER_COUNT
        underTest.onViewEvent(AuthViewEvent.CreateAccountTapped)
        coEvery { userRepository.getUserCount() } throws CancellationException("Screen closed")

        underTest.onViewEvent(AuthViewEvent.CreateAccountTapped)

        assertThat(underTest.uiState.value.userCount, equalTo(EXPECTED_USER_COUNT))
    }

    @Test
    fun `switch to Login mode and clear error message on HaveExistingAccountTapped event`() {
        underTest.onViewEvent(AuthViewEvent.CreateAccountTapped)
        underTest.onViewEvent(AuthViewEvent.HaveExistingAccountTapped)

        val state = underTest.uiState.value
        assertThat(state.authMode, equalTo(AuthMode.Login))
        assertThat(state.errorMessage, equalTo(""))
    }

    @Test
    fun `update state on successful login`() {
        coEvery { loginUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.success(Unit)

        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.LoginTapped)

        val state = underTest.uiState.value
        assertTrue(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(""))
        coVerify(exactly = 1) { loginUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) }
    }

    @Test
    fun `update error message on login failure with custom error message`() {
        val errorMessage = "Invalid username or password"
        coEvery { loginUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.failure(RuntimeException(errorMessage))

        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.LoginTapped)

        val state = underTest.uiState.value
        assertFalse(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(errorMessage))
    }

    @Test
    fun `update error message with default fallback on login failure with null exception message`() {
        coEvery { loginUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.failure(RuntimeException(null as String?))

        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.LoginTapped)

        val state = underTest.uiState.value
        assertFalse(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(AuthViewModel.SIGN_IN_FAILED_MESSAGE))
    }

    @Test
    fun `show error message and do not invoke sign up when passwords do not match`() {
        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(OTHER_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.SignUpTapped)

        val state = underTest.uiState.value
        assertThat(state.errorMessage, equalTo(AuthViewModel.MISMATCHING_PASSWORDS_MESSAGE))
        assertFalse(state.successfulLogin)
        coVerify(exactly = 0) { signUpUseCase.invoke(any(), any()) }
    }

    @Test
    fun `update state on successful sign up when passwords match`() {
        coEvery { signUpUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.success(Unit)

        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.SignUpTapped)

        val state = underTest.uiState.value
        assertTrue(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(""))
        coVerify(exactly = 1) { signUpUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) }
    }

    @Test
    fun `update error message on sign up failure with custom error message`() {
        val errorMessage = "Username is already taken"
        coEvery { signUpUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.failure(RuntimeException(errorMessage))

        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.SignUpTapped)

        val state = underTest.uiState.value
        assertFalse(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(errorMessage))
    }

    @Test
    fun `update error message with default fallback on sign up failure with null exception message`() {
        coEvery { signUpUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.failure(RuntimeException(null as String?))

        underTest.onViewEvent(AuthViewEvent.UsernameUpdated(TEST_USERNAME))
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.ConfirmPasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.SignUpTapped)

        val state = underTest.uiState.value
        assertFalse(state.successfulLogin)
        assertThat(state.errorMessage, equalTo(AuthViewModel.SIGN_UP_FAILED_MESSAGE))
    }

    @Test
    fun `handle sign in with PasswordCredential on OnLoad event`() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentialManager

        val passwordCredential = mockk<PasswordCredential>()
        every { passwordCredential.id } returns TEST_USERNAME
        every { passwordCredential.password } returns TEST_PASSWORD

        val response = mockk<GetCredentialResponse>()
        every { response.credential } returns passwordCredential

        coEvery { credentialManager.getCredential(context = context, request = any()) } returns response
        coEvery { loginUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) } returns Result.success(Unit)

        underTest.onViewEvent(AuthViewEvent.OnLoad(context))

        val state = underTest.uiState.value
        assertThat(state.username, equalTo(TEST_USERNAME))
        assertThat(state.password, equalTo(TEST_PASSWORD))
        assertTrue(state.successfulLogin)
        coVerify(exactly = 1) { loginUseCase.invoke(TEST_USERNAME, TEST_PASSWORD) }
    }

    @Test
    fun `handle sign in with non-password credential on OnLoad event`() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentialManager

        val otherCredential = mockk<Credential>()
        val response = mockk<GetCredentialResponse>()
        every { response.credential } returns otherCredential

        coEvery { credentialManager.getCredential(context = context, request = any()) } returns response

        underTest.onViewEvent(AuthViewEvent.OnLoad(context))

        val state = underTest.uiState.value
        assertThat(state.username, equalTo(""))
        assertThat(state.password, equalTo(""))
        assertFalse(state.successfulLogin)
        coVerify(exactly = 0) { loginUseCase.invoke(any(), any()) }
    }

    @Test
    fun `clear error message when GetCredentialException occurs on OnLoad event`() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentialManager

        val exception = mockk<GetCredentialException>()
        coEvery { credentialManager.getCredential(context = context, request = any()) } throws exception

        underTest.onViewEvent(AuthViewEvent.OnLoad(context))

        val state = underTest.uiState.value
        assertThat(state.errorMessage, equalTo(""))
    }

    @Test
    fun `leave auth state unchanged when credential retrieval is cancelled`() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentialManager
        coEvery { credentialManager.getCredential(context = context, request = any()) } throws
            CancellationException("Screen closed")
        underTest.onViewEvent(AuthViewEvent.PasswordUpdated(TEST_PASSWORD))
        underTest.onViewEvent(AuthViewEvent.SignUpTapped)
        val before = underTest.uiState.value

        underTest.onViewEvent(AuthViewEvent.OnLoad(context))

        assertEquals(before, underTest.uiState.value)
        coVerify(exactly = 0) { loginUseCase.invoke(any(), any()) }
    }

    @Test
    fun `set error message with custom message when Exception occurs on OnLoad event`() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentialManager

        val errorMessage = "Credential manager error"
        coEvery { credentialManager.getCredential(context = context, request = any()) } throws RuntimeException(errorMessage)

        underTest.onViewEvent(AuthViewEvent.OnLoad(context))

        val state = underTest.uiState.value
        assertThat(state.errorMessage, equalTo(errorMessage))
    }

    @Test
    fun `set error message with default fallback when Exception with null message occurs on OnLoad event`() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentialManager

        coEvery { credentialManager.getCredential(context = context, request = any()) } throws RuntimeException(null as String?)

        underTest.onViewEvent(AuthViewEvent.OnLoad(context))

        val state = underTest.uiState.value
        assertThat(state.errorMessage, equalTo(AuthViewModel.CREDENTIAL_MANAGER_FAILED_MESSAGE))
    }

    private companion object {
        const val TEST_USERNAME = "test_user"
        const val TEST_PASSWORD = "test_password"
        const val OTHER_PASSWORD = "other_password"
        const val EXPECTED_USER_COUNT = 5
    }
}
