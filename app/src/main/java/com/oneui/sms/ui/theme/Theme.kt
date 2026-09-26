package com.oneui.sms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = OneUIBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = ColorPrimaryContainer,
    onPrimaryContainer = TextPrimaryLight,
    secondaryContainer = ColorSecondaryContainer,
    onSecondaryContainer = TextPrimaryLight,
    background = SurfaceLight,
    surface = CardLight,
    surfaceVariant = SurfaceDim,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = CardLight,
    surfaceContainerLow = SurfaceLight,
    surfaceContainerHigh = ColorSecondaryContainer,
)

private val AmoledColors = darkColorScheme(
    primary = OneUIBlueDark,
    primaryContainer = ColorPrimaryContainerDark,
    onPrimaryContainer = TextPrimaryDark,
    secondaryContainer = ColorSecondaryContainerDark,
    onSecondaryContainer = TextPrimaryDark,
    background = androidx.compose.ui.graphics.Color.Black,
    surface = androidx.compose.ui.graphics.Color.Black,
    surfaceVariant = SurfaceDimDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
)

private val DarkColors = darkColorScheme(
    primary = OneUIBlueDark,
    primaryContainer = ColorPrimaryContainerDark,
    onPrimaryContainer = TextPrimaryDark,
    secondaryContainer = ColorSecondaryContainerDark,
    onSecondaryContainer = TextPrimaryDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceDimDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = SurfaceDimDark,
    surfaceContainerLow = SurfaceDark,
    surfaceContainerHigh = ColorSecondaryContainerDark,
)

private val OneUITypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val OneUIShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

@Composable
fun OneMessagesTheme(themeMode: String = "system", dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        "dark", "amoled" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        themeMode == "amoled" -> AmoledColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = OneUITypography, shapes = OneUIShapes, content = content)
}
