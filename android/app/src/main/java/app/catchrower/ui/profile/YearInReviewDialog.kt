package app.catchrower.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import app.catchrower.data.YearInReview
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchTextSecondary

@Composable
fun YearInReviewDialog(
    summary: YearInReview.Summary,
    onDone: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDone,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(560.dp),
        title = { Text(summary.title()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary.body(), style = MaterialTheme.typography.bodyLarge)
                summary.detailLine()?.let { detail ->
                    Text(detail, style = MaterialTheme.typography.bodyMedium, color = CatchTextSecondary)
                }
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
