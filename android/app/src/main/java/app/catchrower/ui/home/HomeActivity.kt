package app.catchrower.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.catchrower.data.HouseholdGate
import app.catchrower.ui.CatchDestination
import app.catchrower.ui.CatchViewModel
import app.catchrower.ui.challenges.ChallengesScreen
import app.catchrower.ui.dashboard.DashboardScreen
import app.catchrower.ui.dashboard.SessionSummaryDialog
import app.catchrower.ui.idle.IdleSleepFlag
import app.catchrower.ui.idle.IdleTimeout
import app.catchrower.ui.idle.PanelPower
import app.catchrower.ui.idle.ScreenIdle
import app.catchrower.ui.profile.AddProfileDialog
import app.catchrower.ui.profile.BtDevicePickerDialog
import app.catchrower.ui.profile.ProfilePickerScreen
import app.catchrower.ui.profile.ProfileScreen
import app.catchrower.ui.profile.YearInReviewDialog
import app.catchrower.ui.settings.CatchSettingsScreen
import app.catchrower.ui.settings.PinDialog
import app.catchrower.ui.setup.SetupScreen
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchTheme
import app.catchrower.util.AuroraStore
import app.catchrower.update.UpdateState
import app.catchrower.util.SystemSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val IDLE_SLEEP_KEY = "from_idle"

class HomeActivity : ComponentActivity() {
    private val viewModel: CatchViewModel by viewModels()
    private val screenIdle = ScreenIdle()
    private val idleSleepPrefs by lazy { getSharedPreferences("catch_idle_sleep", MODE_PRIVATE) }
    private val idleSleep by lazy {
        IdleSleepFlag(
            read = { idleSleepPrefs.getBoolean(IDLE_SLEEP_KEY, false) },
            write = { sleeping ->
                idleSleepPrefs.edit().putBoolean(IDLE_SLEEP_KEY, sleeping).commit()
                Unit
            },
        )
    }
    private var panelWasAsleep = false

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> syncPanel(SystemClock.elapsedRealtime())
                Intent.ACTION_USER_PRESENT -> onDeviceWake()
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
        screenIdle.resetClock(SystemClock.elapsedRealtime())
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
        registerReceiver(
            userPresentReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
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
        lifecycleScope.launch {
            launch {
                viewModel.uiState.collect { state ->
                    screenIdle.onTelemetry(
                        strokeCount = state.telemetry.strokeCount,
                        nowElapsedMs = SystemClock.elapsedRealtime(),
                    )
                }
            }
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val state = viewModel.uiState.value
                syncPanel(now)
                val enabled = IdleTimeout.isEnabled(state.settings?.screenOffWhenIdle)
                PanelPower.keepAwakeWhileCounting(window, enabled)
                screenIdle.tick(
                    nowElapsedMs = now,
                    enabled = enabled,
                    hostingMedia = state.embeddedPackage != null,
                )
                if (screenIdle.readyToSleep && PanelPower.isInteractive(this@HomeActivity)) {
                    idleSleep.markSleep()
                    panelWasAsleep = true
                    if (!PanelPower.goToSleep(this@HomeActivity)) {
                        idleSleep.clear()
                        panelWasAsleep = false
                    }
                }
                delay(1_000)
            }
        }
    }

    /**
     * The panel just became interactive again. Restart the idle clock before the
     * next timeout check, so waking does not immediately call [PanelPower.goToSleep].
     */
    private fun syncPanel(nowElapsedMs: Long) {
        val interactive = PanelPower.isInteractive(this)
        if (interactive && panelWasAsleep) {
            screenIdle.onUserInput(nowElapsedMs)
            panelWasAsleep = false
        } else if (!interactive) {
            panelWasAsleep = true
        }
    }

    private fun onDeviceWake() {
        val now = SystemClock.elapsedRealtime()
        syncPanel(now)
        if (idleSleep.consumeKeepSession(now)) {
            screenIdle.onWake(now, viewModel.uiState.value.telemetry.sessionPhase)
        } else {
            viewModel.onUserPresent()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResumeCatch()
        // Coming back from another app starts the idle clock over, so Catch
        // is visible first and only sleeps after a fresh quiet stretch.
        noteInput()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) noteInput()
        return super.dispatchTouchEvent(ev)
    }

    private fun noteInput() {
        screenIdle.onUserInput(SystemClock.elapsedRealtime())
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(userPresentReceiver) }
        runCatching { unregisterReceiver(packageChangeReceiver) }
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        noteInput()
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
    var pinPrompt by remember { mutableStateOf<((String) -> Boolean)?>(null) }

    fun gate(action: HouseholdGate.Action, onGranted: (String?) -> Unit) {
        if (viewModel.allow(action, null)) {
            onGranted(null)
        } else {
            pinPrompt = { pin ->
                if (viewModel.allow(action, pin)) {
                    onGranted(pin)
                    pinPrompt = null
                    true
                } else {
                    false
                }
            }
        }
    }

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
            onCatchSettings = {
                gate(HouseholdGate.Action.OpenCatchSettings) { pin -> viewModel.openCatchSettings(pin) }
            },
            onSystemSettings = onOpenSettings,
            availableUpdate = (updateState as? UpdateState.Available)?.release?.version,
        )
        CatchDestination.Dashboard -> DashboardScreen(
            state = state,
            onSelectProfile = viewModel::selectProfile,
            onCatchSettings = {
                gate(HouseholdGate.Action.OpenCatchSettings) { pin -> viewModel.openCatchSettings(pin) }
            },
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
            onOpenProfile = viewModel::goProfile,
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
            onSave = viewModel::saveCatchSettings,
            onSetAsHome = onSetAsHome,
            onAurora = onOpenAurora,
            onSystemSettings = onOpenSettings,
            updateState = updateState,
            updateChannel = updateChannel,
            onCheckUpdate = { viewModel.checkForUpdate() },
            onInstallUpdate = viewModel::installUpdate,
            onExportJson = { viewModel.exportWorkouts("json") },
            onExportCsv = { viewModel.exportWorkouts("csv") },
            onPrivacyWipe = viewModel::privacyWipe,
            onResetHousehold = { viewModel.resetHouseholdMetrics(confirmed = true) },
            onOpenChallenges = viewModel::goChallenges,
            onResistance = viewModel::setResistance,
        )
        CatchDestination.Challenges -> ChallengesScreen(
            cards = state.challengeCards,
            badges = state.badges,
            onBack = viewModel::leaveChallenges,
        )
        CatchDestination.Profile -> ProfileScreen(
            profile = state.activeProfile,
            workouts = state.workouts,
            badges = state.badges,
            timezoneId = state.settings?.timezoneId,
            onBack = viewModel::leaveProfile,
            onOpenChallenges = viewModel::goChallenges,
            onResetStats = { id -> viewModel.resetProfileStats(id, confirmed = true) },
        )
    }
    }

    val summary by viewModel.sessionSummary.collectAsStateWithLifecycle()
    summary?.let { SessionSummaryDialog(summary = it, onDone = viewModel::dismissSessionSummary) }

    val review = state.yearInReview
    if (
        summary == null &&
        review != null &&
        (state.destination == CatchDestination.Dashboard || state.destination == CatchDestination.Profile) &&
        !state.showBtPicker &&
        !state.showSessionGoalDialog
    ) {
        YearInReviewDialog(summary = review, onDone = viewModel::dismissYearInReview)
    }

    if (state.showAddProfile) {
        AddProfileDialog(
            onDismiss = { viewModel.openAddProfile(false) },
            onCreate = viewModel::createProfile,
        )
    }
    pinPrompt?.let { submit ->
        PinDialog(
            onDismiss = { pinPrompt = null },
            onSubmit = submit,
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
