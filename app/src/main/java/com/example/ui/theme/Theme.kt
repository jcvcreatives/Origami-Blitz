package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PaperCraftColorScheme = lightColorScheme(
    primary = CardboardDark,
    onPrimary = MaskingTapeCream,
    primaryContainer = CardboardLight,
    onPrimaryContainer = InkBrown,
    secondary = OrigamiFoxOrange,
    onSecondary = Color.White,
    secondaryContainer = StickyNoteYellow,
    onSecondaryContainer = InkBrown,
    tertiary = OrigamiCraneBlue,
    onTertiary = InkBrown,
    background = CraftTableKraft,
    onBackground = InkBrown,
    surface = MaskingTapeCream,
    onSurface = InkBrown,
    surfaceVariant = CardboardLight,
    onSurfaceVariant = InkSoftBrown
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PaperCraftColorScheme,
        typography = Typography,
        content = content
    )
}
