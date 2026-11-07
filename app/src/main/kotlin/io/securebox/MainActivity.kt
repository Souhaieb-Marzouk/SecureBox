package io.securebox

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.ui.theme.SecureBoxTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            if (state.isLoading) {
                SecureBoxTheme(darkTheme = true) { LoadingScreen() }
            } else {
                ApplyAppLanguage(state.settings.language)
                SecureBoxRoot(state = state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Usage access, notifications and VPN consent can change in system settings while the app
        // is in the background; refresh so the score and screens stay accurate.
        viewModel.refreshRuntimeState()
    }
}

@Composable
private fun ApplyAppLanguage(language: AppLanguage) {
    LaunchedEffect(language) {
        val desired = language.tag
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != desired) {
            // AppCompat recreates the activity; the ViewModel state (and onboarding step) survives.
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(desired))
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
