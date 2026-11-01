package app.catchrower.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.catchrower.R
import app.catchrower.data.BadgeRules
import app.catchrower.data.RowingCalendar
import app.catchrower.data.StatsReset
import app.catchrower.data.YearInReview
import app.catchrower.data.db.BadgeAwardEntity
import app.catchrower.data.db.ProfileEntity
import app.catchrower.data.db.WorkoutEntity
import app.catchrower.ui.Formatters
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchGold
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextPrimary
import app.catchrower.ui.theme.CatchTextSecondary
import app.catchrower.ui.theme.CatchZone
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * One rower's month, stats, and awards. The calendar is this profile only.
 */
@Composable
fun ProfileScreen(
    profile: ProfileEntity?,
    workouts: List<WorkoutEntity>,
    badges: List<BadgeAwardEntity>,
    timezoneId: String?,
    onBack: () -> Unit,
    onOpenChallenges: () -> Unit,
    onResetStats: (Long) -> Unit,
) {
    val zone = remember(timezoneId) { RowingCalendar.zoneOf(timezoneId) }
    val today = remember(zone) { LocalDate.now(zone) }
    var month by remember(today) { mutableStateOf(RowingCalendar.initialMonth(today)) }
    var confirmReset by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CatchNavyElevated, CatchNavy))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ProfileAvatar(
                        name = profile?.displayName ?: "?",
                        photoUri = profile?.photoUri,
                        size = 56.dp,
                    )
                    Column {
                        Text(
                            profile?.displayName ?: stringResource(R.string.profile),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        ProfileDetails(profile)
                    }
                }
                TextButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }

            if (profile == null) {
                Text(
                    stringResource(R.string.profile_pick_someone),
                    color = CatchTextSecondary,
                )
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MonthCalendar(
                    profileId = profile.id,
                    workouts = workouts,
                    month = month,
                    today = today,
                    zone = zone,
                    onPrevious = { if (RowingCalendar.canGoToPrevious(month)) month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                    modifier = Modifier.weight(1.15f),
                )
                Column(
                    modifier = Modifier.weight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatsPanel(
                        profileId = profile.id,
                        workouts = workouts,
                        month = month,
                        today = today,
                        zone = zone,
                    )
                    AwardsPanel(badges = badges, onOpenChallenges = onOpenChallenges)
                    OutlinedButton(onClick = { confirmReset = true }) {
                        Text(stringResource(R.string.reset_stats))
                    }
                }
            }
        }
    }

    if (confirmReset && profile != null) {
        val warning = StatsReset.warning(StatsReset.Target.Profile, profile.displayName)
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(warning.title) },
            text = { Text(warning.body) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onResetStats(profile.id)
                    },
                ) {
                    Text(stringResource(R.string.reset_stats))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileDetails(profile: ProfileEntity?) {
    if (profile == null) return
    val bits = buildList {
        profile.ageYears?.let { add(stringResource(R.string.profile_age, it)) }
        profile.weightKg?.let { kg ->
            val pounds = kg * LB_PER_KG
            add(stringResource(R.string.profile_weight_lb, pounds.toInt()))
        }
    }
    if (bits.isEmpty()) return
    Text(
        bits.joinToString(", "),
        style = MaterialTheme.typography.bodyMedium,
        color = CatchTextSecondary,
    )
}

@Composable
private fun MonthCalendar(
    profileId: Long,
    workouts: List<WorkoutEntity>,
    month: YearMonth,
    today: LocalDate,
    zone: ZoneId,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val marked = remember(profileId, workouts, month, today, zone) {
        RowingCalendar.markedDays(profileId, workouts, month, today, zone)
    }
    val weeks = remember(month) { RowingCalendar.weeks(month) }
    val canGoBack = RowingCalendar.canGoToPrevious(month)

    GlassPanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPrevious, enabled = canGoBack) {
                    Icon(
                        Icons.Outlined.ChevronLeft,
                        contentDescription = stringResource(R.string.calendar_previous_month),
                        tint = if (canGoBack) CatchTextPrimary else CatchTextSecondary,
                    )
                }
                Text(
                    RowingCalendar.monthTitle(month),
                    style = MaterialTheme.typography.titleLarge,
                )
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = stringResource(R.string.calendar_next_month),
                        tint = CatchTextPrimary,
                    )
                }
            }
            Text(
                stringResource(R.string.calendar_days_rowed),
                style = MaterialTheme.typography.bodyMedium,
                color = CatchTextSecondary,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                RowingCalendar.weekdayLabels().forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = CatchTextSecondary,
                    )
                }
            }
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        DayCell(
                            date = cell.date,
                            lit = cell.date != null && cell.date in marked,
                            isToday = cell.date == today,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    lit: Boolean,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.height(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (date == null) return@Box
        val description = if (lit) {
            stringResource(R.string.calendar_day_rowed, date.dayOfMonth)
        } else {
            date.dayOfMonth.toString()
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        lit -> CatchZone
                        isToday -> CatchAccent.copy(alpha = 0.22f)
                        else -> CatchNavyElevated.copy(alpha = 0.35f)
                    },
                )
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (lit) CatchNavy else CatchTextPrimary,
            )
        }
    }
}

@Composable
private fun StatsPanel(
    profileId: Long,
    workouts: List<WorkoutEntity>,
    month: YearMonth,
    today: LocalDate,
    zone: ZoneId,
) {
    val monthStats = remember(profileId, workouts, month, today, zone) {
        RowingCalendar.monthStats(profileId, workouts, month, today, zone)
    }
    val lifetime = remember(profileId, workouts, zone) {
        RowingCalendar.lifetimeStats(profileId, workouts, zone)
    }
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.profile_this_month), style = MaterialTheme.typography.titleLarge)
            StatLine(monthStats)
            Text(stringResource(R.string.profile_all_sessions), style = MaterialTheme.typography.titleLarge)
            StatLine(lifetime)
        }
    }
}

@Composable
private fun StatLine(stats: RowingCalendar.Stats) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Stat(stringResource(R.string.profile_stat_days), stats.daysRowed.toString(), Modifier.weight(1f))
        Stat(stringResource(R.string.profile_stat_sessions), stats.sessions.toString(), Modifier.weight(1f))
        Stat(
            stringResource(R.string.profile_stat_distance),
            Formatters.metersLabel(stats.distanceMeters),
            Modifier.weight(1.3f),
        )
        Stat(
            stringResource(R.string.profile_stat_time),
            YearInReview.timeLabel(stats.durationSec),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.Start, modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = CatchAccent, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = CatchTextSecondary, maxLines = 1)
    }
}

@Composable
private fun AwardsPanel(
    badges: List<BadgeAwardEntity>,
    onOpenChallenges: () -> Unit,
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.profile_awards), style = MaterialTheme.typography.titleLarge)
            if (badges.isEmpty()) {
                Text(
                    stringResource(R.string.profile_no_awards),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CatchTextSecondary,
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    badges.distinctBy { it.badgeId }.forEach { badge ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.EmojiEvents,
                                contentDescription = null,
                                tint = CatchGold,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                BadgeRules.titleFor(badge.badgeId),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            TextButton(onClick = onOpenChallenges) {
                Text(stringResource(R.string.challenges_programs))
            }
        }
    }
}

private const val LB_PER_KG = 2.2046226218f
