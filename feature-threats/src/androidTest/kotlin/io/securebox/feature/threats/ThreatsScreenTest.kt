package io.securebox.feature.threats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.securebox.core.domain.security.ScanSummary
import io.securebox.core.ui.theme.SecureBoxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThreatsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val state =
        ThreatsUiState(
            loading = false,
            summary =
                ScanSummary(
                    startedAtMillis = 0L,
                    finishedAtMillis = 1000L,
                    scannedApps = 12,
                    highRiskApps = 2,
                    findingsCount = 7,
                    rulesVersion = 1,
                ),
            scheduled = true,
            rulesVersion = 1,
        )

    @Test
    fun showsScanActionAndSchedule() {
        composeRule.setContent {
            SecureBoxTheme(darkTheme = true) {
                ThreatsScreen(state = state, onScan = {}, onScheduled = {}, onOpenHardening = {})
            }
        }
        composeRule.onNodeWithText("Scan now").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Weekly automatic scan"))
        composeRule.onNodeWithText("Weekly automatic scan").assertIsDisplayed()
    }

    @Test
    fun showsHardeningEntryPoint() {
        composeRule.setContent {
            SecureBoxTheme(darkTheme = true) {
                ThreatsScreen(state = state, onScan = {}, onScheduled = {}, onOpenHardening = {})
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Device hardening"))
        composeRule.onNodeWithText("Device hardening").assertIsDisplayed()
        composeRule.onNodeWithText("Check device settings").assertIsDisplayed()
    }
}
