package com.bibletriviaquiz.playandwin.ui.theme

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
    primary = WarmGoldLight,
    onPrimary = RoyalNavy,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = Color.White,
    secondary = WarmGold,
    onSecondary = RoyalNavy,
    secondaryContainer = GoldenBronze,
    onSecondaryContainer = Color.White,
    background = RoyalNavy,
    onBackground = Color.White,
    surface = Color(0xFF112240),
    onSurface = Color.White,
    error = QuizRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueContainer,
    onPrimaryContainer = RoyalBlue,
    secondary = WarmGold,
    onSecondary = RoyalNavy,
    secondaryContainer = WarmGoldContainer,
    onSecondaryContainer = GoldenBronze,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondary,
    outline = SlateCardBorder,
    error = QuizRed,
    onError = Color.White,
    errorContainer = QuizRedContainer,
    onErrorContainer = QuizRedDark
)

@Composable
fun BibleTriviaQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use intentional brand theme
    content: @Composable () -> Unit
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

@Composable
fun BibleQuizRewardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    BibleTriviaQuizTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
