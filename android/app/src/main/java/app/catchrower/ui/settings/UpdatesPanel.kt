package app.catchrower.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.catchrower.BuildConfig
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchTextSecondary
import app.catchrower.update.UpdateChannel
import app.catchrower.update.UpdateState

@Composable
fun UpdatesPanel(
    state: UpdateState,
    channel: UpdateChannel,
    onCheck: () -> Unit,
    onInstall: () -> Unit,
    onChannel: (UpdateChannel) -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Updates", style = MaterialTheme.typography.headlineMedium)
            Text("Catch ${BuildConfig.VERSION_NAME}", color = CatchTextSecondary)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Channel", style = MaterialTheme.typography.titleMedium)
                FilterChip(
                    selected = channel == UpdateChannel.Stable,
                    onClick = { onChannel(UpdateChannel.Stable) },
                    label = { Text("Stable") },
                )
                FilterChip(
                    selected = channel == UpdateChannel.Beta,
                    onClick = { onChannel(UpdateChannel.Beta) },
                    label = { Text("Beta") },
                )
            }
            if (channel == UpdateChannel.Beta) {
                Text(
                    "Beta builds get new features first and may have rough edges.",
                    color = CatchTextSecondary,
                )
            }

            when (state) {
                UpdateState.Idle -> Unit
                UpdateState.Checking -> Text("Checking for updates...", color = CatchTextSecondary)
                UpdateState.UpToDate -> Text("Catch is up to date.", color = CatchAccent)
                is UpdateState.Updated -> Text("Updated to ${state.version}.", color = CatchAccent)
                is UpdateState.Available -> ReleaseSummary("Catch ${state.release.version} is available.", state.release.notes)
                is UpdateState.Downloading -> {
                    Text("Downloading Catch ${state.release.version}...", color = CatchTextSecondary)
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = CatchAccent,
                    )
                }
                is UpdateState.Installing -> Text(
                    "Installing Catch ${state.release.version}. Catch restarts when it finishes.",
                    color = CatchTextSecondary,
                )
                is UpdateState.Failed -> Text(state.message, color = MaterialTheme.colorScheme.error)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val canInstall = state is UpdateState.Available ||
                    (state is UpdateState.Failed && state.release != null)
                val busy = state is UpdateState.Checking ||
                    state is UpdateState.Downloading ||
                    state is UpdateState.Installing
                if (canInstall) {
                    Button(
                        onClick = onInstall,
                        colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                    ) {
                        Text(if (state is UpdateState.Failed) "Try again" else "Install update")
                    }
                }
                OutlinedButton(onClick = onCheck, enabled = !busy) {
                    Text("Check for updates")
                }
            }
        }
    }
}

@Composable
private fun ReleaseSummary(title: String, notes: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = CatchAccent)
    if (notes.isNotBlank()) {
        Text(
            plainNotes(notes),
            color = CatchTextSecondary,
            maxLines = 8,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Changelog Markdown as plain text: wrapped lines rejoined, headings and bullets kept on their own lines. */
internal fun plainNotes(markdown: String): String = buildString {
    fun newLine() {
        if (isNotEmpty() && !endsWith("\n")) append("\n")
    }
    for (line in markdown.lineSequence().map { it.trim() }) {
        when {
            line.isEmpty() -> newLine()
            line.startsWith("#") -> {
                newLine()
                append(line.trimStart('#').trim())
                append("\n")
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                newLine()
                append("• ")
                append(line.drop(2))
            }
            else -> {
                if (isNotEmpty() && !endsWith("\n")) append(" ")
                append(line)
            }
        }
    }
}.trim()
