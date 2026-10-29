package app.catchrower.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.data.db.ProfileEntity
import app.catchrower.ui.profile.ProfileAvatar
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextPrimary

@Composable
fun ProfileChip(
    active: ProfileEntity?,
    profiles: List<ProfileEntity>,
    onSelect: (ProfileEntity) -> Unit,
    onCatchSettings: () -> Unit,
    onSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CatchNavyElevated)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileAvatar(
                name = active?.displayName ?: "?",
                photoUri = active?.photoUri,
                size = 32.dp,
            )
            Text(
                text = active?.displayName ?: "Profile",
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null, tint = CatchTextPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { p ->
                DropdownMenuItem(
                    text = {
                        Text(
                            if (p.id == active?.id) "${p.displayName}  ✓"
                            else p.displayName,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(p)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.catch_settings)) },
                onClick = {
                    expanded = false
                    onCatchSettings()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.system_settings)) },
                onClick = {
                    expanded = false
                    onSystemSettings()
                },
            )
        }
    }
}
