package app.catchrower.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Water
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.catchrower.rower.RowerTelemetry
import app.catchrower.ui.Formatters
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.theme.CatchTextSecondary

@Composable
fun BottomMetricsStrip(
    telemetry: RowerTelemetry,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        corner = 14.dp,
        contentPadding = 12.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomMetric(Icons.Outlined.Bolt, "Power", "${Formatters.watts(telemetry.watts)} W")
            BottomMetric(
                Icons.Outlined.LocalFireDepartment,
                "Calories",
                "${Formatters.kcal(telemetry.calories)} kcal",
            )
            BottomMetric(
                Icons.Outlined.Speed,
                "Split",
                "${Formatters.split(telemetry.splitSecPer500)} /500m",
            )
            BottomMetric(
                Icons.Outlined.Speed,
                "Avg split",
                "${Formatters.split(telemetry.avgSplitSecPer500)} /500m",
            )
            BottomMetric(
                Icons.Outlined.Water,
                "Stroke rate",
                "${Formatters.spm(telemetry.spm)} spm",
            )
            BottomMetric(
                Icons.Outlined.Water,
                "Strokes",
                telemetry.strokeCount.toString(),
            )
            telemetry.heartRateBpm?.let { bpm ->
                BottomMetric(Icons.Outlined.Favorite, "Heart rate", "$bpm bpm")
            }
            if (telemetry.heartRateBpm != null) {
                BottomMetric(
                    Icons.Outlined.SignalCellularAlt,
                    "Zone",
                    Formatters.zoneLabel(telemetry.zone),
                )
            }
        }
    }
}

@Composable
private fun BottomMetric(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = CatchTextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium)
        }
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}
