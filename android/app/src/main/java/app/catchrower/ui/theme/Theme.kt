package app.catchrower.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CatchColorScheme = darkColorScheme(
    primary = CatchAccent,
    onPrimary = CatchNavy,
    secondary = CatchAccentSoft,
    onSecondary = CatchNavy,
    background = CatchNavy,
    onBackground = CatchTextPrimary,
    surface = CatchNavyElevated,
    onSurface = CatchTextPrimary,
    onSurfaceVariant = CatchTextSecondary,
    outline = CatchPanelBorder,
    error = CatchHeart,
    tertiary = CatchGold,
)

@Composable
fun CatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CatchColorScheme,
        typography = CatchTypography,
        content = content,
    )
}

/** Shared glass panel colors for dashboard chrome. */
object CatchGlass {
    val fill: Color = CatchPanel
    val border: Color = CatchPanelBorder
    val highlight: Color = CatchGlassTop
}
