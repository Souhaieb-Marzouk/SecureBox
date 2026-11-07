package io.securebox.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.ui.theme.SecureBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun render(
        state: OnboardingUiState,
        onLanguage: (AppLanguage) -> Unit = {},
        onConfirmDevice: (Boolean) -> Unit = {},
        onContinue: () -> Unit = {},
    ) {
        composeRule.setContent {
            SecureBoxTheme(darkTheme = true) {
                OnboardingScreen(
                    state = state,
                    onSelectLanguage = onLanguage,
                    onContinueLanguage = onContinue,
                    onConfirmDevice = onConfirmDevice,
                    onBack = {},
                    onNotificationResult = {},
                    onVpnResult = {},
                    onContinuePermissions = onContinue,
                    onFinish = {},
                )
            }
        }
    }

    @Test
    fun languageStepShowsBothLanguages() {
        render(OnboardingUiState())
        composeRule.onNodeWithText("English").assertIsDisplayed()
        composeRule.onNodeWithText("Français").assertIsDisplayed()
    }

    @Test
    fun phoneDetectionAsksForConfirmation() {
        render(OnboardingUiState(step = OnboardingStep.DEVICE, detectedTv = false))
        composeRule
            .onNodeWithText("We detected you are using a phone or tablet. Is that correct?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("No, I'm using an Android TV Box").assertIsDisplayed()
    }

    @Test
    fun tvDetectionOffersPhoneOverride() {
        var confirmed: Boolean? = null
        render(
            state = OnboardingUiState(step = OnboardingStep.DEVICE, detectedTv = true),
            onConfirmDevice = { confirmed = it },
        )
        composeRule
            .onNodeWithText("We detected you are using an Android TV Box. Is that correct?")
            .assertIsDisplayed()
        composeRule.onNodeWithText("No, I'm using a phone").performClick()
        assertEquals(false, confirmed)
    }

    @Test
    fun deviceConfirmationAdvancesToPermissions() {
        var confirmed: Boolean? = null
        render(
            state = OnboardingUiState(step = OnboardingStep.DEVICE, detectedTv = true, deviceMode = DeviceMode.TV),
            onConfirmDevice = { confirmed = it },
        )
        composeRule.onNodeWithText("Yes, continue").performClick()
        assertEquals(true, confirmed)
    }
}
