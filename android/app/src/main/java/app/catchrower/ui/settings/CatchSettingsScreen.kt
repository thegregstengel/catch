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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.data.HouseholdGate
import app.catchrower.data.StatsReset
import app.catchrower.data.db.ProfileEntity
import app.catchrower.media.StreamingApps
import app.catchrower.ui.CatchUiState
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.profile.EditProfileDialog
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextSecondary
import app.catchrower.ui.idle.IdleTimeout
import app.catchrower.update.UpdateChannel
import app.catchrower.update.UpdateState

@Composable
fun CatchSettingsScreen(
    state: CatchUiState,
    onBack: () -> Unit,
    onAddProfile: () -> Unit,
    onUpdateProfile: (ProfileEntity) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onSave: (SettingsForm.Parsed) -> Unit,
    onSetAsHome: () -> Unit,
    onAurora: () -> Unit,
    onSystemSettings: () -> Unit,
    updateState: UpdateState,
    updateChannel: UpdateChannel,
    sideloadRequired: Boolean,
    onCheckUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onPrivacyWipe: () -> Unit,
    onResetHousehold: () -> Unit,
    onOpenChallenges: () -> Unit = {},
    onResistance: (Int) -> Unit = {},
) {
    var editing by remember { mutableStateOf<ProfileEntity?>(null) }
    var confirmDeleteId by remember { mutableStateOf<Long?>(null) }
    var confirmHouseholdReset by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val savedGoal = state.settings?.monthlyFamilyGoalMeters ?: 2_000_000
    val savedPin = state.settings?.settingsPin.orEmpty()
    val savedDisabledKey = state.settings?.disabledAppPackages.orEmpty()
    val savedScreenOff = state.settings?.screenOffWhenIdle ?: true
    var goalText by remember(savedGoal) { mutableStateOf(savedGoal.toString()) }
    var pinText by remember(savedPin) { mutableStateOf("") }
    var channel by remember(updateChannel) { mutableStateOf(updateChannel) }
    var screenOffWhenIdle by remember(savedScreenOff) { mutableStateOf(savedScreenOff) }
    var disabled by remember(savedDisabledKey) {
        mutableStateOf(
            savedDisabledKey.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        )
    }

    fun save() {
        when (
            val result = SettingsForm.parse(
                monthlyGoalText = goalText,
                disabledAppPackages = disabled,
                updateChannel = channel,
                pinText = pinText,
                existingPin = savedPin,
                screenOffWhenIdle = screenOffWhenIdle,
            )
        ) {
            is SettingsForm.ParseResult.Ok -> {
                formError = null
                onSave(result.parsed)
            }
            is SettingsForm.ParseResult.Invalid -> formError = result.message
        }
    }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                    Button(
                        onClick = { save() },
                        colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }

            state.statusMessage?.let { msg ->
                Text(msg, color = CatchAccent)
            }
            formError?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error)
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
                    Text(
                        stringResource(R.string.settings_pin_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        stringResource(R.string.settings_pin_body),
                        color = CatchTextSecondary,
                    )
                    Text(
                        stringResource(
                            if (HouseholdGate.isConfigured(savedPin)) {
                                R.string.settings_pin_set
                            } else {
                                R.string.settings_pin_unset
                            },
                        ),
                        color = CatchTextSecondary,
                    )
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { pinText = it.filter { ch -> ch.isDigit() }.take(HouseholdGate.PIN_LENGTH) },
                        label = { Text(stringResource(R.string.settings_pin_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                stringResource(R.string.settings_screen_off),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(
                                    R.string.settings_screen_off_body,
                                    IdleTimeout.TIMEOUT_MINUTES,
                                ),
                                color = CatchTextSecondary,
                            )
                        }
                        Switch(
                            checked = screenOffWhenIdle,
                            onCheckedChange = { screenOffWhenIdle = it },
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
                    OutlinedTextField(
                        value = goalText,
                        onValueChange = { goalText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(stringResource(R.string.monthly_goal_meters)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                                    disabled = if (on) {
                                        disabled - app.packageIds.toSet()
                                    } else {
                                        disabled + app.packageIds
                                    }
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
                channel = channel,
                sideloadRequired = sideloadRequired,
                onCheck = onCheckUpdate,
                onInstall = onInstallUpdate,
                onChannel = { channel = it },
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
                        OutlinedButton(onClick = { confirmHouseholdReset = true }) {
                            Text(stringResource(R.string.reset_household))
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

    if (confirmHouseholdReset) {
        val warning = StatsReset.warning(StatsReset.Target.Household, "")
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmHouseholdReset = false },
            title = { Text(warning.title) },
            text = { Text(warning.body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmHouseholdReset = false
                        onResetHousehold()
                    },
                ) {
                    Text(stringResource(R.string.reset_household))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmHouseholdReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
