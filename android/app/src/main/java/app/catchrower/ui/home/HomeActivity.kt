package app.catchrower.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.catchrower.ui.CatchDestination
import app.catchrower.ui.CatchViewModel
import app.catchrower.ui.challenges.ChallengesScreen
import app.catchrower.ui.dashboard.DashboardScreen
import app.catchrower.ui.dashboard.SessionSummaryDialog
import app.catchrower.ui.profile.AddProfileDialog
import app.catchrower.ui.profile.BtDevicePickerDialog
import app.catchrower.ui.profile.ProfilePickerScreen
import app.catchrower.ui.settings.CatchSettingsScreen
import app.catchrower.ui.setup.SetupScreen
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchTheme
import app.catchrower.util.AuroraStore
import app.catchrower.update.UpdateState
import app.catchrower.util.SystemSettings

class HomeActivity : ComponentActivity() {
    private val viewModel: CatchViewModel by viewModels()

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                viewModel.onUserPresent()
            }
        }
    }

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED,
                -> viewModel.refreshInstalledApps()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Stay on Theme.Catch.Splash (sunset windowBackground) until Compose paints
        // the real UI — avoids black-logo ↔ loading splash ping-pong after bootanim.
        super.onCreate(savedInstanceState)
        // Transparent system bars; dark-scrim style keeps back/home icons light (white).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
        registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
        val pkgFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageChangeReceiver, pkgFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(packageChangeReceiver, pkgFilter)
        }
        setContent {
            CatchTheme {
                CatchRoot(
                    viewModel = viewModel,
                    onSetAsHome = { SystemSettings.openHomePicker(this) },
                    onOpenSettings = { SystemSettings.open(this) },
                    onOpenWifi = { SystemSettings.openWifi(this) },
                    onOpenAurora = { AuroraStore.openOrInstallHelp(this) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeCatch()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(userPresentReceiver) }
        runCatching { unregisterReceiver(packageChangeReceiver) }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                viewModel.adjustVolume(raise = true)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                viewModel.adjustVolume(raise = false)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@Composable
private fun CatchRoot(
    viewModel: CatchViewModel,
    onSetAsHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWifi: () -> Unit,
    onOpenAurora: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val updateChannel by viewModel.updateChannel.collectAsStateWithLifecycle()

    if (!state.bootstrapped) {
        Box(Modifier.fillMaxSize().background(CatchNavy))
        return
    }

    // Keep chrome above the transparent nav / status bars.
    Box(
        Modifier
            .fillMaxSize()
            .background(CatchNavy)
            .systemBarsPadding(),
    ) {
    when (state.destination) {
        CatchDestination.ProfilePicker -> ProfilePickerScreen(
            profiles = state.profiles,
            onSelect = viewModel::selectProfile,
            onAdd = { viewModel.openAddProfile(true) },
            onCatchSettings = viewModel::goCatchSettings,
            onSystemSettings = onOpenSettings,
            availableUpdate = (updateState as? UpdateState.Available)?.release?.version,
        )
        CatchDestination.Dashboard -> DashboardScreen(
            state = state,
            onSelectProfile = viewModel::selectProfile,
            onCatchSettings = viewModel::goCatchSettings,
            onSystemSettings = onOpenSettings,
            onLaunchApp = viewModel::launchStreamingApp,
            onCloseEmbedded = viewModel::clearEmbeddedSession,
            onEmbedUnavailable = viewModel::onEmbedUnavailable,
            onClearEmbedError = viewModel::clearEmbedError,
            onGo = viewModel::openSessionGoalDialog,
            onPause = viewModel::pauseSession,
            onResume = viewModel::resumeSession,
            onFinish = viewModel::finishSession,
            onConfirmSessionGoal = viewModel::confirmSessionGoal,
            onDismissSessionGoal = viewModel::dismissSessionGoalDialog,
            sessionGoalInitialMeters = state.sessionGoalMeters ?: 2_000,
            onOpenChallenges = viewModel::goChallenges,
        )
        CatchDestination.Setup -> SetupScreen(
            state = state,
            onOpenWifi = onOpenWifi,
            onSetAsHome = onSetAsHome,
            onAurora = onOpenAurora,
            onToggleApp = viewModel::toggleAppEnabled,
            onStepChange = viewModel::setSetupStepIndex,
            onCompleteWithProfile = viewModel::completeOnboardingWithProfile,
        )
        CatchDestination.Settings -> CatchSettingsScreen(
            state = state,
            onBack = viewModel::leaveCatchSettings,
            onAddProfile = { viewModel.openAddProfile(true) },
            onUpdateProfile = viewModel::updateProfile,
            onDeleteProfile = viewModel::deleteProfile,
            onWeeklyGoalMiles = viewModel::setWeeklyGoalMiles,
            onMonthlyGoalMiles = viewModel::setMonthlyGoalMiles,
            onToggleApp = viewModel::toggleAppEnabled,
            onSetAsHome = onSetAsHome,
            onAurora = onOpenAurora,
            onSystemSettings = onOpenSettings,
            updateState = updateState,
            updateChannel = updateChannel,
            onCheckUpdate = { viewModel.checkForUpdate() },
            onInstallUpdate = viewModel::installUpdate,
            onUpdateChannel = viewModel::setUpdateChannel,
            onExportJson = { viewModel.exportWorkouts("json") },
            onExportCsv = { viewModel.exportWorkouts("csv") },
            onPrivacyWipe = viewModel::privacyWipe,
            onOpenChallenges = viewModel::goChallenges,
            onResistance = viewModel::setResistance,
        )
        CatchDestination.Challenges -> ChallengesScreen(
            cards = state.challengeCards,
            badges = state.badges,
            onBack = viewModel::leaveChallenges,
        )
    }
    }

    val summary by viewModel.sessionSummary.collectAsStateWithLifecycle()
    summary?.let { SessionSummaryDialog(summary = it, onDone = viewModel::dismissSessionSummary) }

    if (state.showAddProfile) {
        AddProfileDialog(
            onDismiss = { viewModel.openAddProfile(false) },
            onCreate = viewModel::createProfile,
        )
    }
    if (state.showBtPicker) {
        BtDevicePickerDialog(
            remembered = state.rememberedBt,
            onDismiss = viewModel::dismissBtPicker,
            onPick = viewModel::rememberAndConnectBt,
        )
    }
}
