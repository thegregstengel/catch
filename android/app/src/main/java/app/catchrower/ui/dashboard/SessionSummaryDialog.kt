package app.catchrower.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.catchrower.ui.Formatters
import app.catchrower.ui.SessionSummary
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchGold
import app.catchrower.ui.theme.CatchTextSecondary

@Composable
fun SessionSummaryDialog(
    summary: SessionSummary,
    onDone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(720.dp),
        title = { Text(if (summary.goalReached) "Goal reached!" else "Session complete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val distance = buildString {
                    append("${Formatters.meters(summary.distanceMeters)} m")
                    summary.goalMeters?.let { append(" of ${Formatters.meters(it.toFloat())} m") }
                }
                StatGrid(
                    listOf(
                        "Time" to Formatters.elapsed(summary.elapsedSec),
                        "Distance" to distance,
                        "Avg split" to "${Formatters.split(summary.avgSplitSecPer500)} /500m",
                        "Avg stroke rate" to "${Formatters.spm(summary.avgSpm)} spm",
                        "Strokes" to summary.strokeCount.toString(),
                        "Avg power" to "${Formatters.watts(summary.avgWatts)} W",
                        "Calories" to "${Formatters.kcal(summary.calories)} kcal",
                    ),
                )
                HorizontalDivider()
                Achievements(summary)
            }
        },
        confirmButton = {
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
            ) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun StatGrid(stats: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = CatchTextSecondary,
                        )
                        Text(value, style = MaterialTheme.typography.titleLarge)
                    }
                }
                repeat(3 - row.size) { Column(modifier = Modifier.weight(1f)) {} }
            }
        }
    }
}

@Composable
private fun Achievements(summary: SessionSummary) {
    val items = buildList {
        if (summary.goalReached) add(Icons.Outlined.Flag to "Session goal reached")
        if (summary.familyGoalReached) add(Icons.Outlined.Groups to "Family weekly goal reached")
        summary.challengeTiers.forEach { add(Icons.Outlined.EmojiEvents to it) }
        summary.badges.forEach { add(Icons.Outlined.MilitaryTech to "Badge earned: $it") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Earned this session", style = MaterialTheme.typography.titleMedium)
        if (items.isEmpty()) {
            Text(
                "No new goals or badges this time. Every stroke counts toward the next one.",
                color = CatchTextSecondary,
            )
        }
        items.forEach { (icon, text) -> AchievementRow(icon, text) }
    }
}

@Composable
private fun AchievementRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = CatchGold, modifier = Modifier.size(22.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
