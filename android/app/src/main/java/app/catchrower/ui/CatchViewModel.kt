package app.catchrower.ui

import android.app.Application
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.catchrower.data.CatchRepository
import app.catchrower.data.ChallengeCard
import app.catchrower.data.FamilyBoardSnapshot
import app.catchrower.data.db.AppSettingsEntity
import app.catchrower.data.db.BadgeAwardEntity
import app.catchrower.data.db.ProfileBtDeviceEntity
import app.catchrower.data.db.ProfileEntity
import app.catchrower.hr.HeartRateMonitor
import app.catchrower.media.CatchMediaSession
import app.catchrower.media.StreamingApp
import app.catchrower.media.StreamingApps
import app.catchrower.rower.RowerHal
import app.catchrower.rower.RowerHalFactory
import app.catchrower.rower.RowerTelemetry
import app.catchrower.rower.SessionPhase
import app.catchrower.BuildConfig
import app.catchrower.update.UpdateChannel
import app.catchrower.update.UpdateChecker
import app.catchrower.update.UpdateInstaller
import app.catchrower.update.UpdatePrefs
import app.catchrower.update.UpdateState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class CatchDestination {
    ProfilePicker,
    Dashboard,
    Setup,
    Settings,
    Challenges,
}

/** Final numbers and new awards for the session that just finished. */
data class SessionSummary(
    val elapsedSec: Int,
    val distanceMeters: Float,
    val goalMeters: Int?,
    val avgSplitSecPer500: Float?,
    val avgSpm: Float?,
    val avgWatts: Float?,
    val calories: Float?,
    val strokeCount: Int,
    val badges: List<String>,
    val challengeTiers: List<String>,
    val familyGoalReached: Boolean,
) {
    val goalReached: Boolean get() = goalMeters != null && distanceMeters >= goalMeters
}

data class CatchUiState(
    val destination: CatchDestination = CatchDestination.ProfilePicker,
    /** False until settings are read so we do not flash the wrong screen. */
    val bootstrapped: Boolean = false,
    val setupStepIndex: Int = 0,
    val profiles: List<ProfileEntity> = emptyList(),
    val activeProfile: ProfileEntity? = null,
    val settings: AppSettingsEntity? = null,
    val telemetry: RowerTelemetry = RowerTelemetry(),
    val familyBoard: FamilyBoardSnapshot? = null,
    val badges: List<BadgeAwardEntity> = emptyList(),
    val rememberedBt: List<ProfileBtDeviceEntity> = emptyList(),
    val showBtPicker: Boolean = false,
    val showAddProfile: Boolean = false,
    val statusMessage: String? = null,
    val exporting: Boolean = false,
    val installedAppIds: Set<String> = emptySet(),
    val framedAppLabel: String? = null,
    /** When set, the center pane hosts this package on the system-display host. */
    val embeddedPackage: String? = null,
    /** Set when hosting fails; keeps the pane from bouncing back to the grid. */
    val embedError: String? = null,
    val challengeCards: List<ChallengeCard> = emptyList(),
    /** Session distance goal in meters; null until the rower picks one. */
    val sessionGoalMeters: Int? = null,
    val showSessionGoalDialog: Boolean = false,
)

class CatchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CatchRepository(app)
    private val hrMonitor = HeartRateMonitor(app)
    private val mediaSession = CatchMediaSession(app)

    private val rower: RowerHal = RowerHalFactory.create(viewModelScope)

    private val _destination = MutableStateFlow(CatchDestination.Setup)
    private val _bootstrapped = MutableStateFlow(false)
    private val _setupStepIndex = MutableStateFlow(0)
    private val _showBtPicker = MutableStateFlow(false)
    private val _showAddProfile = MutableStateFlow(false)
    private val _workoutId = MutableStateFlow<Long?>(null)
    private val _statusMessage = MutableStateFlow<String?>(null)
    private val _exporting = MutableStateFlow(false)
    private val _rememberedBt = MutableStateFlow<List<ProfileBtDeviceEntity>>(emptyList())
    private val _badges = MutableStateFlow<List<BadgeAwardEntity>>(emptyList())
    private val _installedAppIds = MutableStateFlow<Set<String>>(emptySet())
    private val _framedAppLabel = MutableStateFlow<String?>(null)
    private val _embeddedPackage = MutableStateFlow<String?>(null)
    private val _embedError = MutableStateFlow<String?>(null)
    private val _challengeCards = MutableStateFlow<List<ChallengeCard>>(emptyList())
    private val _sessionGoalMeters = MutableStateFlow<Int?>(null)
    private val _showSessionGoalDialog = MutableStateFlow(false)
    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary: StateFlow<SessionSummary?> = _sessionSummary.asStateFlow()
    private val updatePrefs = UpdatePrefs(app)
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    private val _updateChannel = MutableStateFlow(updatePrefs.channel)
    val updateChannel: StateFlow<UpdateChannel> = _updateChannel.asStateFlow()

    private val boardFlow = repo.observeFamilyBoard()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<CatchUiState> = combine(
        combine(
            _destination,
            _bootstrapped,
            _setupStepIndex,
            repo.observeProfiles(),
            repo.observeActiveProfile(),
        ) { dest, bootstrapped, setupStep, profiles, active ->
            Quint(dest, bootstrapped, setupStep, profiles, active)
        },
        combine(
            repo.observeSettings(),
            rower.telemetry,
            boardFlow,
            _badges,
            _rememberedBt,
        ) { settings, telemetry, board, badges, bt ->
            Quint(settings, telemetry, board, badges, bt)
        },
        combine(
            _showBtPicker,
            _showAddProfile,
            _statusMessage,
            _exporting,
            hrMonitor.bpm,
        ) { showBt, showAdd, status, exporting, hr ->
            Quint(showBt, showAdd, status, exporting, hr)
        },
        combine(
            _installedAppIds,
            _framedAppLabel,
            _embeddedPackage,
            _embedError,
            _challengeCards,
        ) { installed, framed, embedded, embedError, cards ->
            Quint(installed, framed, embedded, embedError, cards)
        },
        combine(_sessionGoalMeters, _showSessionGoalDialog, ::Pair),
    ) { a, b, c, d, e ->
        val hr = c.e
        val telem = b.b.copy(heartRateBpm = hr ?: b.b.heartRateBpm)
        CatchUiState(
            destination = a.a,
            bootstrapped = a.b,
            setupStepIndex = a.c,
            profiles = a.d,
            activeProfile = a.e,
            settings = b.a,
            telemetry = telem,
            familyBoard = b.c,
            badges = b.d,
            rememberedBt = b.e,
            showBtPicker = c.a,
            showAddProfile = c.b,
            statusMessage = c.c,
            exporting = c.d,
            installedAppIds = d.a,
            framedAppLabel = d.b,
            embeddedPackage = d.c,
            embedError = d.d,
            challengeCards = d.e,
            sessionGoalMeters = e.first,
            showSessionGoalDialog = e.second,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CatchUiState())

    init {
        refreshInstalledApps()
        noteUpdateCompleted()
        viewModelScope.launch {
            UpdateInstaller.failure.collect { message ->
                val current = _updateState.value
                if (message != null && current is UpdateState.Installing) {
                    _updateState.value = UpdateState.Failed(message, current.release)
                }
            }
        }
        viewModelScope.launch {
            repo.ensureSettings()
            val s = repo.getSettings()
            _destination.value = if (s.onboardingComplete) {
                CatchDestination.ProfilePicker
            } else {
                CatchDestination.Setup
            }
            _bootstrapped.value = true
            runCatching { rower.start() }
            mediaSession.activate()
        }
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            repo.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) flowOf(emptyList())
                    else repo.observeBtDevices(profile.id)
                }
                .collect { list ->
                    _rememberedBt.value = list
                    val hrMac = list.firstOrNull { it.kind == "hr" }?.macAddress
                    hrMonitor.setPreferredMac(hrMac)
                }
        }
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            repo.observeActiveProfile()
                .flatMapLatest { profile ->
                    if (profile == null) flowOf(emptyList())
                    else repo.observeBadges(profile.id)
                }
                .collect { _badges.value = it }
        }
        viewModelScope.launch {
            rower.telemetry.collect { t ->
                val id = _workoutId.value ?: return@collect
                repo.updateLiveWorkout(
                    workoutId = id,
                    distanceMeters = t.distanceMeters,
                    durationSec = t.elapsedSec,
                    avgSplitSecPer500 = t.avgSplitSecPer500,
                    avgSpm = t.spm,
                    avgWatts = t.avgWatts,
                    calories = t.calories,
                    strokeCount = t.strokeCount,
                    maxHeartRate = t.heartRateBpm,
                    avgHeartRate = t.heartRateBpm,
                )
                rower.setHeartRate(hrMonitor.bpm.value)
            }
        }
    }

    fun onUserPresent() {
        viewModelScope.launch {
            val s = repo.getSettings()
            if (!s.onboardingComplete) {
                // Unlock / home relaunch mid-wizard must not yank to the profile picker.
                _destination.value = CatchDestination.Setup
                return@launch
            }
            endWorkoutIfNeeded()
            repo.clearActiveProfile()
            _destination.value = CatchDestination.ProfilePicker
            clearEmbeddedSession()
        }
    }

    fun selectProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repo.setActiveProfile(profile.id)
            _showBtPicker.value = true
            _destination.value = CatchDestination.Dashboard
            startWorkout(profile.id)
            refreshChallengeCards()
        }
    }

    fun dismissBtPicker() {
        _showBtPicker.value = false
    }

    fun rememberAndConnectBt(mac: String, name: String, kind: String) {
        viewModelScope.launch {
            val profile = uiState.value.activeProfile ?: return@launch
            repo.rememberBtDevice(profile.id, mac, name, kind)
            if (kind == "hr") {
                hrMonitor.setPreferredMac(mac)
            }
            _statusMessage.value = "Remembered $name"
            _showBtPicker.value = false
        }
    }

    fun openAddProfile(show: Boolean = true) {
        _showAddProfile.value = show
    }

    fun createProfile(
        name: String,
        photoUri: String?,
        weightKg: Float?,
        ageYears: Int?,
        sex: String?,
    ) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            repo.addProfile(name, photoUri, weightKg, ageYears, sex)
            _showAddProfile.value = false
            _statusMessage.value = "Profile created"
        }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            if (profile.displayName.isBlank()) return@launch
            repo.updateProfile(profile)
            _statusMessage.value = "Profile updated"
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repo.deleteProfile(id)
            _statusMessage.value = "Profile removed"
        }
    }

    /** First-run wizard: create the initial profile and open the framed home. */
    fun completeOnboardingWithProfile(
        name: String,
        photoUri: String?,
        weightKg: Float?,
        ageYears: Int?,
        sex: String?,
    ) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            val id = repo.addProfile(name, photoUri, weightKg, ageYears, sex)
            repo.updateSettings { it.copy(onboardingComplete = true, activeProfileId = id) }
            _showAddProfile.value = false
            _showBtPicker.value = false
            _setupStepIndex.value = 0
            _statusMessage.value = null
            startWorkout(id)
            _destination.value = CatchDestination.Dashboard
        }
    }

    fun setSetupStepIndex(index: Int) {
        _setupStepIndex.value = index.coerceAtLeast(0)
    }

    fun goCatchSettings() {
        _destination.value = CatchDestination.Settings
    }

    fun goChallenges() {
        viewModelScope.launch {
            refreshChallengeCards()
            _destination.value = CatchDestination.Challenges
        }
    }

    fun leaveChallenges() {
        _destination.value = CatchDestination.Dashboard
    }

    private suspend fun refreshChallengeCards() {
        val profileId = uiState.value.activeProfile?.id ?: run {
            _challengeCards.value = emptyList()
            return
        }
        repo.refreshChallengeProgress(profileId)
        _challengeCards.value = repo.challengeCardsFor(profileId)
    }

    fun leaveCatchSettings() {
        viewModelScope.launch {
            val s = repo.getSettings()
            _destination.value = when {
                !s.onboardingComplete -> CatchDestination.Setup
                s.activeProfileId != null -> CatchDestination.Dashboard
                else -> CatchDestination.ProfilePicker
            }
        }
    }

    fun clearEmbeddedSession() {
        _framedAppLabel.value = null
        _embeddedPackage.value = null
        _embedError.value = null
    }

    fun setResistance(level: Int) {
        rower.setResistance(level)
    }

    fun openSessionGoalDialog() {
        if (rower.telemetry.value.sessionPhase != SessionPhase.Idle) return
        _showSessionGoalDialog.value = true
    }

    fun dismissSessionSummary() {
        _sessionSummary.value = null
    }

    fun dismissSessionGoalDialog() {
        _showSessionGoalDialog.value = false
    }

    fun confirmSessionGoal(goalMeters: Int) {
        val goal = goalMeters.coerceIn(250, 20_000)
        _sessionGoalMeters.value = goal
        _showSessionGoalDialog.value = false
        rower.resetSession()
        rower.startSession()
    }

    fun pauseSession() {
        rower.pauseSession()
    }

    fun resumeSession() {
        if (rower.telemetry.value.sessionPhase == SessionPhase.Paused) {
            rower.startSession()
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            rower.pauseSession()
            val t = rower.telemetry.value
            val goal = _sessionGoalMeters.value
            val outcome = _workoutId.value?.let { id ->
                _workoutId.value = null
                repo.finishWorkout(id).also { refreshChallengeCards() }
            }
            _sessionSummary.value = SessionSummary(
                elapsedSec = t.elapsedSec,
                distanceMeters = t.distanceMeters,
                goalMeters = goal,
                avgSplitSecPer500 = t.avgSplitSecPer500,
                avgSpm = if (t.elapsedSec > 0) t.strokeCount * 60f / t.elapsedSec else null,
                avgWatts = t.avgWatts,
                calories = t.calories,
                strokeCount = t.strokeCount,
                badges = outcome?.badgeTitles.orEmpty(),
                challengeTiers = outcome?.challengeTiers.orEmpty(),
                familyGoalReached = outcome?.familyGoalReached == true,
            )
            rower.resetSession()
            val profile = uiState.value.activeProfile
            if (profile != null) {
                startWorkout(profile.id)
            }
        }
    }

    fun refreshInstalledApps() {
        _installedAppIds.value = StreamingApps.installedIds(getApplication())
    }

    fun onResumeCatch() {
        refreshInstalledApps()
        checkForUpdateIfDue()
    }

    fun launchStreamingApp(app: StreamingApp) {
        val ctx = getApplication<Application>()
        val pkg = StreamingApps.resolveInstalledPackage(ctx, app)
        if (pkg == null) {
            StreamingApps.launchOrAurora(ctx, app)
            mediaSession.activate()
            return
        }

        mediaSession.activate()

        if (StreamingApps.launchIntent(ctx, app) == null) {
            _statusMessage.value = "Cannot host ${app.label} in frame on this build"
            _embeddedPackage.value = null
            _framedAppLabel.value = null
            return
        }

        _embedError.value = null
        _framedAppLabel.value = app.label
        _embeddedPackage.value = pkg
        _statusMessage.value = null
    }

    /** Hosting failed: keep the pane open with an error; never swap chrome. */
    fun onEmbedUnavailable(reason: String = "Could not embed in the Catch frame") {
        _embeddedPackage.value = null
        _embedError.value = reason
        _statusMessage.value = reason
    }

    fun clearEmbedError() {
        _embedError.value = null
        _statusMessage.value = null
    }

    fun adjustVolume(raise: Boolean) {
        mediaSession.adjustVolume(
            if (raise) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
        )
    }

    fun setWeeklyGoalMiles(miles: Float) {
        if (miles <= 0f) return
        viewModelScope.launch { repo.setWeeklyFamilyGoal((miles * METERS_PER_MILE).toInt()) }
    }

    fun setMonthlyGoalMiles(miles: Float) {
        if (miles <= 0f) return
        viewModelScope.launch { repo.setMonthlyFamilyGoal((miles * METERS_PER_MILE).toInt()) }
    }

    fun toggleAppEnabled(packageId: String, enabled: Boolean) {
        viewModelScope.launch {
            val s = repo.getSettings()
            val set = repo.disabledAppSet(s).toMutableSet()
            if (enabled) set.remove(packageId) else set.add(packageId)
            repo.setDisabledApps(set)
        }
    }

    /** Checks at most once a day on its own; [checkForUpdate] always checks. */
    fun checkForUpdateIfDue() {
        if (System.currentTimeMillis() - updatePrefs.lastCheckEpochMs < UPDATE_CHECK_INTERVAL_MS) return
        val state = _updateState.value
        if (state !is UpdateState.Idle && state !is UpdateState.Updated) return
        checkForUpdate(quiet = true)
    }

    fun checkForUpdate(quiet: Boolean = false) {
        viewModelScope.launch {
            if (!quiet) {
                updatePrefs.updatedTo = null
                _updateState.value = UpdateState.Checking
            }
            val before = _updateState.value
            _updateState.value = runCatching { UpdateChecker.latest(_updateChannel.value) }
                .fold(
                    onSuccess = { release ->
                        updatePrefs.lastCheckEpochMs = System.currentTimeMillis()
                        when {
                            release != null -> UpdateState.Available(release)
                            quiet -> before.takeIf { it is UpdateState.Updated } ?: UpdateState.Idle
                            else -> UpdateState.UpToDate
                        }
                    },
                    onFailure = {
                        if (quiet) before else UpdateState.Failed("Could not reach GitHub. Check the Wi-Fi connection.")
                    },
                )
        }
    }

    fun setUpdateChannel(channel: UpdateChannel) {
        if (channel == _updateChannel.value) return
        updatePrefs.channel = channel
        _updateChannel.value = channel
        checkForUpdate()
    }

    fun installUpdate() {
        val release = when (val state = _updateState.value) {
            is UpdateState.Available -> state.release
            is UpdateState.Failed -> state.release ?: return
            else -> return
        }
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                _updateState.value = UpdateState.Downloading(release, 0f)
                val apk = UpdateInstaller.download(app, release) { progress ->
                    _updateState.value = UpdateState.Downloading(release, progress)
                }
                _updateState.value = UpdateState.Installing(release)
                UpdateInstaller.install(app, apk)
            } catch (e: Exception) {
                _updateState.value = UpdateState.Failed(e.message ?: "The update failed.", release)
            }
        }
    }

    private fun noteUpdateCompleted() {
        val previous = updatePrefs.lastRunVersion
        updatePrefs.lastRunVersion = BuildConfig.VERSION_NAME
        if (previous != null && previous != BuildConfig.VERSION_NAME) {
            updatePrefs.updatedTo = BuildConfig.VERSION_NAME
        }
        if (updatePrefs.updatedTo == BuildConfig.VERSION_NAME) {
            _updateState.value = UpdateState.Updated(BuildConfig.VERSION_NAME)
        }
    }

    fun exportWorkouts(format: String) {
        viewModelScope.launch {
            _exporting.value = true
            try {
                val text = if (format == "csv") repo.exportWorkoutsCsv() else repo.exportWorkoutsJson()
                val dir = getApplication<Application>().getExternalFilesDir(null)
                    ?: getApplication<Application>().filesDir
                val file = File(dir, "catch-workouts.$format")
                file.writeText(text)
                _statusMessage.value = "Exported to ${file.absolutePath}"
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            } finally {
                _exporting.value = false
            }
        }
    }

    fun privacyWipe() {
        viewModelScope.launch {
            endWorkoutIfNeeded()
            repo.privacyWipe()
            _destination.value = CatchDestination.Setup
            _statusMessage.value = "All Catch personal data wiped"
        }
    }

    private suspend fun startWorkout(profileId: Long) {
        endWorkoutIfNeeded()
        _workoutId.value = repo.startWorkout(profileId)
    }

    private suspend fun endWorkoutIfNeeded() {
        val id = _workoutId.value ?: return
        repo.endWorkout(id)
        _workoutId.value = null
        refreshChallengeCards()
    }

    override fun onCleared() {
        viewModelScope.launch {
            endWorkoutIfNeeded()
            rower.stop()
        }
        hrMonitor.disconnect()
        mediaSession.release()
        super.onCleared()
    }

    private companion object {
        const val METERS_PER_MILE = 1609.344f
        const val UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }

    private data class Quint<A, B, C, D, E>(
        val a: A,
        val b: B,
        val c: C,
        val d: D,
        val e: E,
    )
}
