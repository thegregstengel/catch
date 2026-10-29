package app.catchrower.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import app.catchrower.data.db.ProfileEntity
import app.catchrower.rower.SessionPhase
import app.catchrower.ui.CatchUiState
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated

@Composable
fun DashboardScreen(
    state: CatchUiState,
    onSelectProfile: (ProfileEntity) -> Unit,
    onCatchSettings: () -> Unit,
    onSystemSettings: () -> Unit,
    onLaunchApp: (app.catchrower.media.StreamingApp) -> Unit,
    onCloseEmbedded: () -> Unit,
    onEmbedUnavailable: (String) -> Unit,
    onClearEmbedError: () -> Unit,
    onGo: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onConfirmSessionGoal: (Int) -> Unit,
    onDismissSessionGoal: () -> Unit,
    sessionGoalInitialMeters: Int,
    onOpenChallenges: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CatchNavyElevated, CatchNavy))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ProfileChip(
                        active = state.activeProfile,
                        profiles = state.profiles,
                        onSelect = onSelectProfile,
                        onCatchSettings = onCatchSettings,
                        onSystemSettings = onSystemSettings,
                    )
                    SessionControls(
                        phase = state.telemetry.sessionPhase,
                        onGo = onGo,
                        onPause = onPause,
                        onResume = onResume,
                        onFinish = onFinish,
                    )
                }
                ScenicStrip(
                    distanceMeters = state.telemetry.distanceMeters,
                    goalMeters = state.sessionGoalMeters,
                    elapsedSec = state.telemetry.elapsedSec.takeIf {
                        state.telemetry.sessionPhase != SessionPhase.Idle
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.5f),
                )
                // Same width as FamilyBoardPane so the clock centers over it.
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(280.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TopClock()
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MediaHostPane(
                    disabledPackages = state.settings
                        ?.disabledAppPackages
                        ?.split(',')
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.toSet()
                        .orEmpty(),
                    installedAppIds = state.installedAppIds,
                    embeddedPackage = state.embeddedPackage,
                    embeddedLabel = state.framedAppLabel,
                    embedError = state.embedError,
                    onLaunch = onLaunchApp,
                    onCloseEmbedded = onCloseEmbedded,
                    onEmbedUnavailable = onEmbedUnavailable,
                    onClearEmbedError = onClearEmbedError,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
                FamilyBoardPane(
                    board = state.familyBoard,
                    activeProfile = state.activeProfile,
                    badges = state.badges,
                    challengeCards = state.challengeCards,
                    onOpenChallenges = onOpenChallenges,
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(12.dp))
            BottomMetricsStrip(telemetry = state.telemetry)
        }

        if (state.showSessionGoalDialog) {
            SessionGoalDialog(
                initialMeters = sessionGoalInitialMeters,
                onDismiss = onDismissSessionGoal,
                onRow = onConfirmSessionGoal,
            )
        }
    }
}
