package app.catchrower.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.catchrower.data.BadgeRules
import app.catchrower.data.ChallengeCard
import app.catchrower.data.FamilyBoardSnapshot
import app.catchrower.data.db.BadgeAwardEntity
import app.catchrower.data.db.ProfileEntity
import app.catchrower.ui.Formatters
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.profile.ProfileAvatar
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchGold
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextSecondary

@Composable
fun FamilyBoardPane(
    board: FamilyBoardSnapshot?,
    activeProfile: ProfileEntity?,
    badges: List<BadgeAwardEntity>,
    challengeCards: List<ChallengeCard> = emptyList(),
    onOpenChallenges: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier.fillMaxHeight(), corner = 16.dp, contentPadding = 14.dp) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Text("Family challenge", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = when (val d = board?.daysLeft) {
                    null -> ""
                    0 -> "Last day"
                    1 -> "1 day left"
                    else -> "$d days left"
                }.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = CatchAccent,
            )
            board?.lastWeekWinnerName?.let { winner ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "Last week: $winner",
                    style = MaterialTheme.typography.labelMedium,
                    color = CatchTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            val entries = board?.entries.orEmpty().take(5)
            if (entries.isEmpty()) {
                Text(
                    "Log meters to fill the board.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                entries.forEach { entry ->
                    val isYou = activeProfile?.id == entry.profile.id
                    LeaderRow(
                        name = entry.profile.displayName,
                        photoUri = entry.profile.photoUri,
                        meters = entry.meters,
                        highlight = isYou,
                        crown = entry.rank == 1,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("This week's goal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            val total = board?.familyTotalMeters ?: 0f
            val goal = (board?.weeklyGoalMeters ?: 500_000).toFloat()
            val progress = (total / goal).coerceIn(0f, 1f)
            Text(
                "${Formatters.meters(total)} / ${Formatters.meters(goal)} m",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = CatchAccent,
                trackColor = CatchNavyElevated,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${(progress * 100).toInt()}% family total",
                style = MaterialTheme.typography.labelMedium,
            )
            val nextGoal = challengeCards
                .filter { !it.completed && it.def.id != "week_meters_family" }
                .maxByOrNull { it.progressFraction }
            if (nextGoal != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "${nextGoal.def.title}: ${(nextGoal.progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = CatchAccent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(
                onClick = onOpenChallenges,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Goals & awards")
            }
            Spacer(Modifier.weight(1f))
            BadgeRow(badges = badges)
        }
    }
}

@Composable
private fun LeaderRow(
    name: String,
    photoUri: String?,
    meters: Float,
    highlight: Boolean,
    crown: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlight) CatchAccent.copy(alpha = 0.18f) else CatchNavyElevated.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ProfileAvatar(
            name = name,
            photoUri = photoUri,
            size = 36.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (crown) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.WorkspacePremium,
                        contentDescription = "First place",
                        tint = CatchGold,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${Formatters.meters(meters)} m",
                style = MaterialTheme.typography.titleMedium,
                color = CatchAccent,
            )
        }
    }
}

@Composable
private fun BadgeRow(badges: List<BadgeAwardEntity>) {
    val earned = badges.map { it.badgeId }.toSet()
    val defs = BadgeRules.DISTANCE_MILESTONES.take(4)
    val earnedCount = defs.count { it.id in earned }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Badges", style = MaterialTheme.typography.titleMedium)
            Text(
                "$earnedCount / ${defs.size}",
                style = MaterialTheme.typography.labelMedium,
                color = CatchTextSecondary,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            defs.forEach { def ->
                val unlocked = def.id in earned
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (unlocked) CatchGold.copy(alpha = 0.25f)
                                else CatchNavyElevated,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (unlocked) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                            contentDescription = def.title,
                            tint = if (unlocked) CatchGold else CatchTextSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        BadgeRules.titleFor(def.id),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
