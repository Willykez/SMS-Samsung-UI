package com.oneui.sms.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = OneUIBlue,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceDim,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    surfaceContainer = CardLight, // grouped-settings cards
)

private val DarkColors = darkColorScheme(
    primary = OneUIBlueDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDimDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
)

// One UI favors soft, heavily rounded containers over sharp grids.
val OneUIShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun OneMessagesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = OneUIShapes,
        content = content,
    )
}
