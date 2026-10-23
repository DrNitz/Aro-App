package com.drnitz.aro.ui.intake

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.drnitz.aro.data.db.DatabaseTestBase
import com.drnitz.aro.data.db.FixedDateProvider
import com.drnitz.aro.data.repository.RoomAroRepository
import com.drnitz.aro.theme.AroTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** UI regression tests for the undo path on the intake history screen. */
class IntakeHistoryScreenTest : DatabaseTestBase() {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository by lazy { RoomAroRepository(db, FixedDateProvider(LocalDate.now())) }

    @Test
    fun deletingWaterCanBeUndoneFromTheSnackbar() {
        runBlocking {
            repository.addWaterLog(
                date = LocalDate.now(),
                amountMl = 250,
                loggedAt = Instant.now(),
            )
        }

        composeTestRule.setContent {
            AroTheme {
                IntakeHistoryScreen(repository = repository)
            }
        }

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("250 mL").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Delete 250 mL water entry").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Undo").performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("250 mL").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun deletingFastCanBeUndoneFromTheSnackbar() {
        val endedAt = Instant.now()
        runBlocking {
            repository.addFastingSession(
                targetHours = 16,
                startedAt = endedAt.minusSeconds(16 * 3600),
                endedAt = endedAt,
                note = "Felt strong",
            )
        }

        composeTestRule.setContent {
            AroTheme {
                IntakeHistoryScreen(repository = repository)
            }
        }

        composeTestRule.onNodeWithText("Fasting").performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Felt strong").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription("Delete fast from", substring = true)
            .performClick()
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Undo").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Undo").performClick()

        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithText("Felt strong").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
