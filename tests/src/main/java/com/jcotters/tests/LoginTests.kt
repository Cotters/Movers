package com.jcotters.tests

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.jcotters.auth.ui.AuthViewState
import com.jcotters.auth.ui.LoginScreen
import com.jcotters.movers.ui.theme.MoversTheme
import org.junit.Rule
import org.junit.Test

class LoginTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginButtonDisabledWhenAnyInputFieldsEmpty() {
        composeTestRule.setContent {
            MoversTheme {
                LoginScreen(
                    viewState = AuthViewState(),
                    onViewEvent = { },
                )
            }
        }
        composeTestRule
            .onNode(hasClickAction() and hasText("Login"))
            .assertIsNotEnabled()
    }
}