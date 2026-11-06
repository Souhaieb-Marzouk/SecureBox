package io.securebox.core.tv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.securebox.core.ui.theme.SecureBoxTheme
import androidx.tv.material3.MaterialTheme as TvMaterialTheme

/**
 * 10-foot typography: body text never below 18sp (we use 20sp), headlines at 40sp so the score is
 * readable from a couch.
 */
val TvTypography: Typography =
    Typography(
        displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 72.sp, lineHeight = 80.sp),
        headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 30.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 30.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    )

/** Spacing scale for the living-room UI. */
object TvDimens {
    val ScreenPadding = 48.dp
    val SidebarWidth = 260.dp
    val FocusRing = 4.dp
    val CardShape = 20.dp
}

/**
 * Nests the Compose for TV theme inside the shared SecureBox theme so TV material components and
 * our shared design-system components can be used side by side.
 */
@Composable
fun TvSecureBoxTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    TvMaterialTheme {
        SecureBoxTheme(darkTheme = darkTheme, typography = TvTypography) {
            content()
        }
    }
}
