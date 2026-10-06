package com.pavel.c8calculations.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

private const val LINE_HEIGHT_FACTOR = 0.7f

private val BaseTypography = Typography()

private fun compact(style: TextStyle): TextStyle = style.copy(
    lineHeight = style.lineHeight * LINE_HEIGHT_FACTOR,
)

private val CompactTypography = Typography(
    displayLarge = compact(BaseTypography.displayLarge),
    displayMedium = compact(BaseTypography.displayMedium),
    displaySmall = compact(BaseTypography.displaySmall),
    headlineLarge = compact(BaseTypography.headlineLarge),
    headlineMedium = compact(BaseTypography.headlineMedium),
    headlineSmall = compact(BaseTypography.headlineSmall),
    titleLarge = compact(BaseTypography.titleLarge),
    titleMedium = compact(BaseTypography.titleMedium),
    titleSmall = compact(BaseTypography.titleSmall),
    bodyLarge = compact(BaseTypography.bodyLarge),
    bodyMedium = compact(BaseTypography.bodyMedium),
    bodySmall = compact(BaseTypography.bodySmall),
    labelLarge = compact(BaseTypography.labelLarge),
    labelMedium = compact(BaseTypography.labelMedium),
    labelSmall = compact(BaseTypography.labelSmall),
)

@Composable
fun C8CalculationsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        typography = CompactTypography,
        content = content,
    )
}
