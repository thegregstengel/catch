package app.catchrower.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.ui.Formatters
import app.catchrower.ui.theme.CatchAccent
import kotlin.math.roundToInt

private val PRESETS_METERS = listOf(500, 1_000, 2_000, 5_000, 10_000)

@Composable
fun SessionGoalDialog(
    initialMeters: Int,
    onDismiss: () -> Unit,
    onRow: (goalMeters: Int) -> Unit,
) {
    var meters by remember {
        mutableFloatStateOf(initialMeters.coerceIn(250, 20_000).toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_goal_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.session_goal_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${Formatters.meters(meters)} m  ·  ${Formatters.milesFromMeters(meters)} mi",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CatchAccent,
                )
                Slider(
                    value = meters,
                    onValueChange = { meters = (it / 100f).roundToInt() * 100f },
                    valueRange = 250f..20_000f,
                    steps = ((20_000 - 250) / 100) - 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PRESETS_METERS.forEach { preset ->
                        FilterChip(
                            selected = meters.roundToInt() == preset,
                            onClick = { meters = preset.toFloat() },
                            label = {
                                Text(
                                    if (preset >= 1000) "${preset / 1000}k" else preset.toString(),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onRow(meters.roundToInt().coerceIn(250, 20_000)) },
                colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
            ) {
                Text(stringResource(R.string.session_goal_row))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
