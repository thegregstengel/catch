package app.catchrower.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.catchrower.ui.theme.CatchGlass
import app.catchrower.ui.theme.CatchGlassBottom
import app.catchrower.ui.theme.CatchGlassTop

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(CatchGlassTop, CatchGlass.fill, CatchGlassBottom),
                ),
            )
            .border(1.dp, CatchGlass.border, shape)
            .padding(contentPadding),
        content = content,
    )
}
