package app.catchrower.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.catchrower.R
import app.catchrower.ui.Formatters
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextPrimary
import app.catchrower.ui.theme.CatchTextSecondary
import kotlinx.coroutines.delay

@Composable
fun ScenicStrip(
    distanceMeters: Float,
    goalMeters: Int?,
    modifier: Modifier = Modifier,
    /** Session stopwatch; null hides it (no session in progress). */
    elapsedSec: Int? = null,
) {
    val goal = goalMeters?.coerceAtLeast(1)?.toFloat()
    val done = distanceMeters.coerceAtLeast(0f)
    val progress = if (goal != null) (done / goal).coerceIn(0f, 1f) else 0f
    val mark = ImageBitmap.imageResource(R.drawable.catch_icon)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CatchNavyElevated),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Image(
                    bitmap = mark,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                    // catch_icon.png is 2x this size, so it downsamples cleanly.
                    modifier = Modifier.size(width = 84.dp, height = 54.dp),
                )
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    color = CatchTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 6.sp,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (elapsedSec != null) {
                    Text(
                        text = Formatters.elapsed(elapsedSec),
                        style = MaterialTheme.typography.titleMedium,
                        color = CatchTextPrimary,
                    )
                }
                Text(
                    text = if (goal != null) {
                        "${Formatters.meters(done)} / ${Formatters.meters(goal)} m"
                    } else {
                        stringResource(R.string.session_goal_unset)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = CatchTextPrimary,
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(CatchAccent.copy(alpha = 0.2f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceAtLeast(0.02f))
                    .background(CatchAccent),
            )
        }
    }
}

@Composable
fun TopClock(
    modifier: Modifier = Modifier,
) {
    val now by produceState(java.util.Date()) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000)
            value = java.util.Date()
        }
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(now),
            style = MaterialTheme.typography.titleMedium,
            color = CatchTextPrimary,
        )
        Text(
            java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.US).format(now),
            style = MaterialTheme.typography.labelMedium,
            color = CatchTextSecondary,
        )
    }
}
