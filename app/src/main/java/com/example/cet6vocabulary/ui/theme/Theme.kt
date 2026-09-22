package com.example.cet6vocabulary.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val Cet6LightColorScheme = lightColorScheme(
    primary = Cet6Primary,
    onPrimary = Cet6Surface,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = Cet6TextPrimary,
    secondary = Cet6Secondary,
    onSecondary = Cet6Surface,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = Cet6TextPrimary,
    background = Cet6Background,
    onBackground = Cet6TextPrimary,
    surface = Cet6Surface,
    onSurface = Cet6TextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Cet6TextSecondary,
    outline = Cet6Divider,
    error = Cet6Error,
    onError = Cet6Surface,
    errorContainer = Color(0xFFFFE4E4),
    onErrorContainer = Color(0xFF7A1B1B)
)

private val Cet6DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = Color(0xFFFF9A9A),
    onError = Color(0xFF5F1010),
    errorContainer = Color(0xFF7A1B1B),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val Cet6Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun CET6VocabularyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> Cet6DarkColorScheme
        else -> Cet6LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Cet6Typography,
        shapes = Cet6Shapes,
        content = content
    )
}
