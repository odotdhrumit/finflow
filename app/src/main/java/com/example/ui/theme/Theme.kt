package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FinFlowSoftPurple,
    onPrimary = Color(0xFF130E1E),
    primaryContainer = Color(0xFF38205C),
    onPrimaryContainer = Color(0xFFEADDF9),
    secondary = FinFlowSecondaryPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF35204C),
    onSecondaryContainer = FinFlowSoftPurple,
    tertiary = FinFlowSoftBlue,
    onTertiary = Color(0xFF130E1E),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    error = DarkNegativeRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FinFlowPrimaryPurple,              // #6F2DBD (Primary buttons, selected nav, actions)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF0E5FA),
    onPrimaryContainer = FinFlowPrimaryPurple,
    secondary = FinFlowSecondaryPurple,          // #A663CC (Secondary actions, highlights)
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5EDFA),
    onSecondaryContainer = FinFlowSecondaryPurple,
    tertiary = FinFlowSoftPurple,                // #B298DC (Subtle supporting UI)
    onTertiary = Color.White,
    tertiaryContainer = FinFlowSoftBlue,         // #B8D0EB
    onTertiaryContainer = FinFlowPrimaryPurple,
    background = FinFlowBackground,              // #FAF9FE (Clean, very light background)
    onBackground = FinFlowTextPrimary,
    surface = FinFlowSurface,                    // #FFFFFF (Clean white cards)
    onSurface = FinFlowTextPrimary,
    surfaceVariant = FinFlowSurfaceVariant,      // #F3EFFB (Light tinted surfaces)
    onSurfaceVariant = FinFlowTextSecondary,
    outline = FinFlowBorder,                     // #E4DFEE (Soft subtle borders)
    error = FinFlowExpense,
    errorContainer = FinFlowExpenseLight,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
