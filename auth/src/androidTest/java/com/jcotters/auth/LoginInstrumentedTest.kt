package com.jcotters.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTextExactly
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jcotters.auth.ui.AuthViewEvent
import com.jcotters.auth.ui.AuthViewState
import com.jcotters.auth.ui.LoginScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testLoginButtonEnabledOnlyWhenValidInput() {
        var viewState by mutableStateOf(AuthViewState())
        composeTestRule.setContent {
            LoginScreen(
                viewState = viewState,
                onViewEvent = { event ->
                    when (event) {
                        is AuthViewEvent.UsernameUpdated -> {
                            viewState = viewState.copy(username = event.username)
                        }
                        is AuthViewEvent.PasswordUpdated -> {
                            viewState = viewState.copy(password = event.password)
                        }
                        else -> {}
                    }
                },
            )
        }
        val loginButton = composeTestRule.onNode(hasClickAction() and hasTextExactly("Login"))
        loginButton
            .assertIsNotEnabled()
        composeTestRule.onNode(hasContentDescription("Username input"))
            .performTextInput("test@example.com")
        composeTestRule.onNode(hasContentDescription("Password input"))
            .performTextInput("password")

        loginButton.assertIsEnabled()
    }
}