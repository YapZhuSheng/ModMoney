package com.modmoney.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Palette: amber/orange as the "money" accent against a cool graphite ground,
// with a blue secondary for spec/technical information.
private val Amber200 = Color(0xFFFFCC80)
private val Amber300 = Color(0xFFFFB74D)
private val Amber700 = Color(0xFFF57C00)
private val Blue200 = Color(0xFF90CAF9)
private val Blue300 = Color(0xFF64B5F6)
private val Blue700 = Color(0xFF1976D2)
private val Teal200 = Color(0xFF80CBC4)
private val Teal700 = Color(0xFF00796B)

private val Ink = Color(0xFF0F1115)
private val InkElevated = Color(0xFF161A21)
private val InkCard = Color(0xFF1C212A)
private val InkOutline = Color(0xFF39414F)
private val OnInk = Color(0xFFE6E9EF)
private val OnInkMuted = Color(0xFFAEB6C4)

private val DarkColors = darkColorScheme(
    primary = Amber300,
    onPrimary = Color(0xFF3E2600),
    primaryContainer = Color(0xFF573A00),
    onPrimaryContainer = Amber200,
    secondary = Blue300,
    onSecondary = Color(0xFF00325B),
    secondaryContainer = Color(0xFF11406B),
    onSecondaryContainer = Blue200,
    tertiary = Teal200,
    onTertiary = Color(0xFF00382F),
    tertiaryContainer = Color(0xFF005045),
    onTertiaryContainer = Color(0xFF9CF0E3),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Ink,
    onBackground = OnInk,
    surface = Ink,
    onSurface = OnInk,
    surfaceVariant = InkCard,
    onSurfaceVariant = OnInkMuted,
    surfaceContainerLowest = Color(0xFF0A0C10),
    surfaceContainerLow = InkElevated,
    surfaceContainer = InkCard,
    surfaceContainerHigh = Color(0xFF232935),
    surfaceContainerHighest = Color(0xFF2A313E),
    outline = InkOutline,
    outlineVariant = Color(0xFF2A303B),
    inverseSurface = OnInk,
    inverseOnSurface = Ink
)

// A light scheme is provided so the app still looks correct if the user
// explicitly switches, but dark is the default (see ModMoneyTheme).
private val LightColors = lightColorScheme(
    primary = Amber700,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB3),
    onPrimaryContainer = Color(0xFF2A1800),
    secondary = Blue700,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E4FF),
    onSecondaryContainer = Color(0xFF001C38),
    tertiary = Teal700,
    onTertiary = Color.White,
    background = Color(0xFFFBF9F7),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFBF9F7),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFEDE7E1),
    onSurfaceVariant = Color(0xFF4C4639)
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium
    ),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
)

/**
 * App theme.
 *
 * Documented decision: dark mode is the default regardless of the system
 * setting — [darkTheme] defaults to true rather than to
 * [isSystemInDarkTheme]. Dynamic color is intentionally not used so the
 * brand palette stays consistent across devices.
 */
@Composable
fun ModMoneyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                window.statusBarColor = colors.background.toArgb()
                window.navigationBarColor = colors.background.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

/** Semantic accent colors used by status and severity chips. */
object AccentColors {
    val planned = Color(0xFF9AA4B2)
    val bought = Color(0xFFFFB74D)
    val installed = Color(0xFF66BB6A)

    val info = Color(0xFF64B5F6)
    val caution = Color(0xFFFFB74D)
    val critical = Color(0xFFEF5350)

    val high = Color(0xFFEF5350)
    val medium = Color(0xFFFFB74D)
    val low = Color(0xFF78909C)
}
