package io.securebox.feature.apps

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.AppSecurityInfo
import io.securebox.core.domain.security.Finding
import io.securebox.core.domain.security.FindingCode
import io.securebox.core.domain.security.InstallSource
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.ui.theme.SecureBoxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun report(
        packageName: String,
        label: String,
        score: Int,
        level: RiskLevel,
    ): AppRiskReport =
        AppRiskReport(
            app =
                AppSecurityInfo(
                    packageName = packageName,
                    label = label,
                    uid = 1000,
                    versionName = "1.0",
                    isSystem = false,
                    source = InstallSource.PLAY_STORE,
                    installer = "com.android.vending",
                    firstInstallTimeMillis = 0L,
                    lastUpdateTimeMillis = 0L,
                    permissions = listOf("android.permission.CAMERA"),
                ),
            score = score,
            level = level,
            findings = listOf(Finding(FindingCode.CAMERA_ACCESS, RiskLevel.MEDIUM)),
        )

    private val state =
        AppsUiState(
            loading = false,
            reports =
                listOf(
                    report("com.bank", "Banking", 50, RiskLevel.MEDIUM),
                    report("com.game", "Game", 100, RiskLevel.SAFE),
                ),
        )

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    fun listsInstalledAppsWithRisk() {
        composeRule.setContent {
            SecureBoxTheme(darkTheme = true) {
                AppsScreen(state = state, onFilter = {}, onSelect = {}, onRefresh = {})
            }
        }
        scrollTo("Banking")
        composeRule.onNodeWithText("Banking").assertIsDisplayed()
        scrollTo("Game")
        composeRule.onNodeWithText("Game").assertIsDisplayed()
    }

    @Test
    fun riskyFilterHidesSafeApps() {
        val filter = mutableStateOf(AppsFilter.ALL)
        composeRule.setContent {
            SecureBoxTheme(darkTheme = true) {
                AppsScreen(
                    state = state.copy(filter = filter.value),
                    onFilter = { filter.value = it },
                    onSelect = {},
                    onRefresh = {},
                )
            }
        }
        composeRule.onNodeWithText("Risky").performClick()
        scrollTo("Banking")
        composeRule.onNodeWithText("Banking").assertIsDisplayed()
        composeRule.onNodeWithText("Game").assertDoesNotExist()
    }
}
