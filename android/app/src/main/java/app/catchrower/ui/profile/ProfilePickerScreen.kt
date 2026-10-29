package app.catchrower.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.data.db.ProfileEntity
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextSecondary

/**
 * Wake / unlock screen: profiles and settings, all centered.
 */
@Composable
fun ProfilePickerScreen(
    profiles: List<ProfileEntity>,
    onSelect: (ProfileEntity) -> Unit,
    onAdd: () -> Unit,
    onCatchSettings: () -> Unit,
    onSystemSettings: () -> Unit,
    /** Version of an update waiting to be installed, if any. */
    availableUpdate: String? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CatchNavyElevated, CatchNavy))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                stringResource(R.string.who_is_rowing),
                style = MaterialTheme.typography.displayLarge,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                profiles.forEach { profile ->
                    WakeProfileButton(
                        name = profile.displayName,
                        photoUri = profile.photoUri,
                        onClick = { onSelect(profile) },
                    )
                }
                WakeAddButton(onClick = onAdd)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onCatchSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.catch_settings))
                }
                OutlinedButton(onClick = onSystemSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.system_settings))
                }
            }

            if (availableUpdate != null) {
                TextButton(onClick = onCatchSettings) {
                    Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = CatchAccent)
                    Spacer(Modifier.size(8.dp))
                    Text("Catch $availableUpdate is available. Install it from Catch Settings.", color = CatchAccent)
                }
            }
        }
    }
}

@Composable
private fun WakeProfileButton(
    name: String,
    photoUri: String?,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        ProfileAvatar(
            name = name,
            photoUri = photoUri,
            size = 96.dp,
        )
        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun WakeAddButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(CatchNavyElevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = stringResource(R.string.add_profile),
                tint = CatchAccent,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.add),
            style = MaterialTheme.typography.titleMedium,
            color = CatchTextSecondary,
        )
    }
}
