package com.drnitz.aro.ui.main

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroHeader
import org.junit.Rule
import org.junit.Test

/** UI tests for the main screen's persistent header. */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appHeader_exists() {
        composeTestRule.setContent {
            AroTheme {
                AroHeader(onOpenNotifications = {}, onOpenGoals = {})
            }
        }

        composeTestRule.onNodeWithText("Aro").assertExists()
        composeTestRule.onNodeWithText("by Dr.Nitz").assertExists()
    }
}
