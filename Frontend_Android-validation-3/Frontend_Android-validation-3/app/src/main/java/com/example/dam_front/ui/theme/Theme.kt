package com.example.dam_front.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HeaderBlue,
    secondary = IconGreen,
    tertiary = IconOrange,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

private val LightColorScheme = lightColorScheme(
    primary = HeaderBlue,
    secondary = IconGreen,
    tertiary = IconOrange,
    background = CardWhite,
    surface = CardWhite,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextBlue,
    onSurface = TextBlue,
)

@Composable
fun DAM_frontTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}



