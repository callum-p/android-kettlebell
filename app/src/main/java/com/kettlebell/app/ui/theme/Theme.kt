package com.kettlebell.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Brand palette — energetic violet with a warm amber accent.
private val Violet = Color(0xFF5B4EE0)
private val VioletDark = Color(0xFF4335C4)
private val Amber = Color(0xFFF5A524)
private val Coral = Color(0xFFFF6B6B)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF160066),
    secondary = Amber,
    onSecondary = Color(0xFF3A2600),
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF2A1B00),
    tertiary = Coral,
    onTertiary = Color.White,
    background = Color(0xFFF7F4FB),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFECE7F3),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC3BCFF),
    onPrimary = Color(0xFF160066),
    primaryContainer = VioletDark,
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFFFFC46B),
    onSecondary = Color(0xFF3A2600),
    secondaryContainer = Color(0xFF6B4A00),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFFFF9A9A),
    onTertiary = Color(0xFF5F1414),
    background = Color(0xFF121016),
    onBackground = Color(0xFFE6E1E9),
    surface = Color(0xFF1D1B22),
    onSurface = Color(0xFFE6E1E9),
    surfaceVariant = Color(0xFF48454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF948F99),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun KettlebellTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
