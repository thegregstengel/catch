package app.catchrower.ui.setup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.catchrower.R
import app.catchrower.media.StreamingApps
import app.catchrower.ui.CatchUiState
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.profile.AvatarPickerGrid
import app.catchrower.ui.profile.DefaultAvatars
import app.catchrower.ui.profile.ProfileAvatar
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextSecondary
import app.catchrower.util.AuroraStore

private enum class SetupStep {
    Wifi,
    Home,
    Aurora,
    AppGrid,
    Profile,
}

private const val LB_PER_KG = 2.2046226218f

@Composable
fun SetupScreen(
    state: CatchUiState,
    onOpenWifi: () -> Unit,
    onSetAsHome: () -> Unit,
    onAurora: () -> Unit,
    onToggleApp: (packageId: String, enabled: Boolean) -> Unit,
    onStepChange: (Int) -> Unit,
    onCompleteWithProfile: (
        name: String,
        photoUri: String?,
        weightKg: Float?,
        ageYears: Int?,
        sex: String?,
    ) -> Unit,
) {
    val steps = SetupStep.entries
    val stepIndex = state.setupStepIndex.coerceIn(0, steps.lastIndex)
    val step = steps[stepIndex]
    val progress = (stepIndex + 1).toFloat() / steps.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CatchNavyElevated, CatchNavy))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 48.dp, vertical = 36.dp)
                .widthIn(max = 720.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                stringResource(R.string.catch_setup),
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                stringResource(R.string.setup_step_of, stepIndex + 1, steps.size),
                style = MaterialTheme.typography.bodyMedium,
                color = CatchTextSecondary,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = CatchAccent,
            )

            GlassPanel(modifier = Modifier.fillMaxWidth(), corner = 18.dp) {
                when (step) {
                    SetupStep.Wifi -> WifiStep(onOpenWifi = onOpenWifi)
                    SetupStep.Home -> HomeStep(onSetAsHome = onSetAsHome)
                    SetupStep.Aurora -> AuroraStep(onAurora = onAurora)
                    SetupStep.AppGrid -> AppGridStep(state = state, onToggleApp = onToggleApp)
                    SetupStep.Profile -> ProfileStep(
                        onComplete = onCompleteWithProfile,
                    )
                }
            }

            if (step != SetupStep.Profile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (stepIndex > 0) {
                        OutlinedButton(onClick = { onStepChange(stepIndex - 1) }) {
                            Text(stringResource(R.string.setup_back))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { onStepChange(stepIndex + 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
                    ) {
                        Text(stringResource(R.string.setup_continue))
                    }
                }
            } else if (stepIndex > 0) {
                OutlinedButton(onClick = { onStepChange(stepIndex - 1) }) {
                    Text(stringResource(R.string.setup_back))
                }
            }
        }
    }
}

@Composable
private fun WifiStep(onOpenWifi: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.setup_wifi_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.setup_wifi_body), color = CatchTextSecondary)
        Button(
            onClick = onOpenWifi,
            colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
        ) {
            Text(stringResource(R.string.setup_wifi_action))
        }
    }
}

@Composable
private fun HomeStep(onSetAsHome: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.setup_home_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.set_as_home_hint), color = CatchTextSecondary)
        Button(
            onClick = onSetAsHome,
            colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
        ) {
            Text(stringResource(R.string.set_as_home))
        }
    }
}

@Composable
private fun AuroraStep(onAurora: () -> Unit) {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(AuroraStore.isInstalled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                installed = AuroraStore.isInstalled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.setup_aurora_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            if (installed) {
                stringResource(R.string.aurora_ready)
            } else {
                stringResource(R.string.aurora_missing)
            },
            color = CatchTextSecondary,
        )
        Button(
            onClick = onAurora,
            colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
        ) {
            Text(
                if (installed) {
                    stringResource(R.string.open_aurora)
                } else {
                    stringResource(R.string.setup_aurora_install)
                },
            )
        }
    }
}

@Composable
private fun AppGridStep(
    state: CatchUiState,
    onToggleApp: (packageId: String, enabled: Boolean) -> Unit,
) {
    val disabled = state.settings
        ?.disabledAppPackages
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        .orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.setup_apps_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.setup_apps_body), color = CatchTextSecondary)
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

@Composable
private fun ProfileStep(
    onComplete: (String, String?, Float?, Int?, String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var weightLb by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf<String?>(null) }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) photoUri = uri.toString()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.setup_profile_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.setup_profile_body), color = CatchTextSecondary)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.setup_profile_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(
                name = name.ifBlank { "?" },
                photoUri = photoUri,
                size = 72.dp,
            )
            OutlinedButton(
                onClick = { pickPhoto.launch("image/*") },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    if (photoUri != null && !DefaultAvatars.isDefault(photoUri)) {
                        stringResource(R.string.setup_profile_photo_selected)
                    } else {
                        stringResource(R.string.setup_profile_photo)
                    },
                )
            }
        }
        AvatarPickerGrid(
            selectedUri = photoUri,
            onSelect = { photoUri = it },
        )
        OutlinedTextField(
            value = weightLb,
            onValueChange = { weightLb = it.filter { ch -> ch.isDigit() || ch == '.' } },
            label = { Text(stringResource(R.string.setup_profile_weight)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = age,
            onValueChange = { age = it.filter { ch -> ch.isDigit() } },
            label = { Text(stringResource(R.string.setup_profile_age)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.setup_profile_sex), color = CatchTextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = sex == "M",
                onClick = { sex = if (sex == "M") null else "M" },
                label = { Text("M") },
            )
            FilterChip(
                selected = sex == "F",
                onClick = { sex = if (sex == "F") null else "F" },
                label = { Text("F") },
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onComplete(
                    name.trim(),
                    photoUri,
                    weightLb.toFloatOrNull()?.div(LB_PER_KG),
                    age.toIntOrNull(),
                    sex,
                )
            },
            enabled = name.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = CatchAccent),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.finish_setup))
        }
    }
}
