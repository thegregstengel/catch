package app.catchrower.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.data.db.ProfileEntity
import app.catchrower.media.StreamingApps
import app.catchrower.ui.CatchUiState
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.profile.EditProfileDialog
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextSecondary
import app.catchrower.update.UpdateChannel
import app.catchrower.update.UpdateState

private const val METERS_PER_MILE = 1609.344f

@Composable
fun CatchSettingsScreen(
    state: CatchUiState,
    onBack: () -> Unit,
    onAddProfile: () -> Unit,
    onUpdateProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onWeeklyGoalMiles: (Float) -> Unit,
    onMonthlyGoalMiles: (Float) -> Unit,
    onToggleApp: (packageId: String, enabled: Boolean) -> Unit,
    onSetAsHome: () -> Unit,
    onAurora: () -> Unit,
    onSystemSettings: () -> Unit,
    updateState: UpdateState,
    updateChannel: UpdateChannel,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onUpdateChannel: (UpdateChannel) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onPrivacyWipe: () -> Unit,
    onOpenChallenges: () -> Unit = {},
    onResistance: (Int) -> Unit = {},
) {
    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }

    val weeklyMiles = (state.settings?.weeklyFamilyGoalMeters ?: 500_000) / METERS_PER_MILE
    val monthlyMiles = (state.settings?.monthlyFamilyGoalMeters ?: 2_000_000) / METERS_PER_MILE
    var weeklyText by remember(weeklyMiles) {
        mutableStateOf("%.0f".format(weeklyMiles))
    }
    var monthlyText by remember(monthlyMiles) {
        mutableStateOf("%.0f".format(monthlyMiles))
    }

    val disabled = state.settings
        ?.disabledAppPackages
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        .orEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CatchNavyElevated, CatchNavy))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 36.dp, vertical = 28.dp)
                .widthIn(max = 900.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.catch_settings),
                    style = MaterialTheme.typography.displayLarge,
                )
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }

            state.statusMessage?.let { msg ->
                Text(msg, color = CatchAccent)
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_profiles),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    state.profiles.forEach { profile ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                            Row {
                                TextButton(onClick = { editing = profile }) {
                                    Text(stringResource(R.string.edit))
                                }
                                TextButton(onClick = { confirmDeleteId = profile.id }) {
                                    Text(stringResource(R.string.remove))
                                }
                            }
                        }
                    }
                    Button(
                        onClick = onAddProfile,
                        colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                    ) {
                        Text(stringResource(R.string.add_profile))
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Rower", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Resistance", style = MaterialTheme.typography.titleMedium)
                        Slider(
                            value = state.telemetry.resistance.toFloat(),
                            onValueChange = { onResistance(it.toInt()) },
                            valueRange = 1f..20f,
                            steps = 18,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${state.telemetry.resistance}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CatchAccent,
                        )
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_family_goals),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        stringResource(R.string.settings_family_goals_body),
                        color = CatchTextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = weeklyText,
                            onValueChange = { weeklyText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.weekly_goal_miles)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                weeklyText.toFloatOrNull()?.let { onWeeklyGoalMiles(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = monthlyText,
                            onValueChange = { monthlyText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text(stringResource(R.string.monthly_goal_miles)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                monthlyText.toFloatOrNull()?.let { onMonthlyGoalMiles(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                    Button(
                        onClick = onOpenChallenges,
                        colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                    ) {
                        Text(stringResource(R.string.challenges_programs))
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_apps),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(stringResource(R.string.settings_apps_body), color = CatchTextSecondary)
                    StreamingApps.DAY_ONE.forEach { app ->
                        val enabled = app.packageIds.none { it in disabled }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = enabled,
                                onCheckedChange = { on ->
                                    app.packageIds.forEach { onToggleApp(it, on) }
                                },
                            )
                        }
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_home),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onSetAsHome) {
                            Text(stringResource(R.string.set_as_home))
                        }
                        OutlinedButton(onClick = onAurora) {
                            Text(stringResource(R.string.open_aurora))
                        }
                        OutlinedButton(onClick = onSystemSettings) {
                            Text(stringResource(R.string.system_settings))
                        }
                    }
                }
            }

            UpdatesPanel(
                state = updateState,
                channel = updateChannel,
                onCheck = onCheckUpdate,
                onInstall = onInstallUpdate,
                onChannel = onUpdateChannel,
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_data),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onExportJson, enabled = !state.exporting) {
                            Text(stringResource(R.string.export_json))
                        }
                        OutlinedButton(onClick = onExportCsv, enabled = !state.exporting) {
                            Text(stringResource(R.string.export_csv))
                        }
                        OutlinedButton(onClick = onPrivacyWipe) {
                            Text(stringResource(R.string.privacy_wipe))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    editing?.let { profile ->
        EditProfileDialog(
            profile = profile,
            onDismiss = { editing = null },
            onSave = {
                onUpdateProfile(it)
                editing = null
            },
        )
    }

    confirmDeleteId?.let { id ->
        val name = state.profiles.firstOrNull { it.id == id }?.displayName.orEmpty()
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            title = { Text(stringResource(R.string.remove_profile_title)) },
            text = { Text(stringResource(R.string.remove_profile_body, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProfile(id)
                        confirmDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteId = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
