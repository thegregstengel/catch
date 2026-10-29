package app.catchrower.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.catchrower.rower.SessionPhase
import app.catchrower.ui.theme.CatchAccent

private val PauseAmber = Color(0xFFF59E0B)
private val FinishRed = Color(0xFFE11D48)

/** Go, or Pause/Go plus Finish while a session is running or paused. */
@Composable
fun SessionControls(
    phase: SessionPhase,
    onGo: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        when (phase) {
            SessionPhase.Idle -> SessionButton("Go", Icons.Outlined.PlayArrow, CatchAccent, onGo)
            SessionPhase.Running -> {
                SessionButton("Pause", Icons.Outlined.Pause, PauseAmber, onPause)
                SessionButton("Finish", Icons.Outlined.Flag, FinishRed, onFinish)
            }
            SessionPhase.Paused -> {
                SessionButton("Go", Icons.Outlined.PlayArrow, CatchAccent, onResume)
                SessionButton("Finish", Icons.Outlined.Flag, FinishRed, onFinish)
            }
        }
    }
}

@Composable
private fun SessionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .height(44.dp)
            .widthIn(min = 120.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1, softWrap = false)
    }
}
