package app.catchrower.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.catchrower.embed.HostedDisplayView
import app.catchrower.media.StreamingApp
import app.catchrower.media.StreamingApps
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchGlass
import app.catchrower.ui.theme.CatchTextSecondary

@Composable
fun MediaHostPane(
    disabledPackages: Set<String>,
    installedAppIds: Set<String>,
    embeddedPackage: String?,
    embeddedLabel: String?,
    embedError: String?,
    onLaunch: (StreamingApp) -> Unit,
    onCloseEmbedded: () -> Unit,
    onEmbedUnavailable: (String) -> Unit,
    onClearEmbedError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (embeddedPackage != null) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = modifier
                .fillMaxHeight()
                .background(CatchGlass.fill, shape)
                .border(1.dp, CatchGlass.border, shape)
                .padding(16.dp),
        ) {
            EmbedHeader(
                title = embeddedLabel ?: "Now playing",
                subtitle = "Playing in this window",
                trailing = {
                    TextButton(onClick = onCloseEmbedded) {
                        Icon(Icons.Outlined.Close, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Close")
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
            HostedDisplayView(
                packageName = embeddedPackage,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onFailed = onEmbedUnavailable,
            )
        }
        return
    }

    GlassPanel(modifier = modifier.fillMaxHeight(), corner = 16.dp, contentPadding = 16.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            EmbedHeader(
                title = if (embedError != null) "Could not open" else "Watch and row",
                subtitle = embedError ?: "Tap an app to open it here",
                trailing = {
                    if (embedError != null) {
                        TextButton(onClick = onClearEmbedError) {
                            Text("Back")
                        }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StreamingApps.DAY_ONE.forEach { app ->
                    val installed = app.id in installedAppIds
                    val disabled = app.packageIds.any { it in disabledPackages }
                    if (disabled) return@forEach
                    AppTile(
                        app = app,
                        installed = installed,
                        onClick = { onLaunch(app) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmbedHeader(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CatchTextSecondary,
            )
        }
        trailing()
    }
}

@Composable
private fun AppTile(
    app: StreamingApp,
    installed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        modifier = modifier.clickable(onClick = onClick),
        corner = 14.dp,
        contentPadding = 12.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = if (installed) Icons.Outlined.Movie else Icons.Outlined.Shop,
                contentDescription = null,
                tint = CatchAccent,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(app.label, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (installed) "Open" else "Get in Aurora",
                style = MaterialTheme.typography.labelMedium,
            )
            if (installed) {
                Spacer(Modifier.height(8.dp))
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = CatchAccent)
            }
        }
    }
}
