package app.catchrower.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.catchrower.data.BadgeRules
import app.catchrower.data.ChallengeCard
import app.catchrower.data.ChallengeCatalog
import app.catchrower.data.db.BadgeAwardEntity
import app.catchrower.ui.Formatters
import app.catchrower.ui.components.GlassPanel
import app.catchrower.ui.theme.CatchAccent
import app.catchrower.ui.theme.CatchGold
import app.catchrower.ui.theme.CatchNavy
import app.catchrower.ui.theme.CatchNavyElevated
import app.catchrower.ui.theme.CatchTextSecondary

/**
 * Always-on goals and awards. No join step: progress comes from workouts.
 */
@Composable
fun ChallengesScreen(
    cards: List<ChallengeCard>,
    badges: List<BadgeAwardEntity>,
    onBack: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val inProgress = cards.filter { !it.completed }
    val done = cards.filter { it.completed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CatchNavy)
            .padding(24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Goals & awards", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Always on. Row and they fill in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CatchTextSecondary,
                )
            }
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
        Spacer(Modifier.height(12.dp))
        TabRow(
            selectedTabIndex = tab,
            containerColor = CatchNavyElevated,
            contentColor = CatchAccent,
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("In progress") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Completed") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Badges") })
        }
        Spacer(Modifier.height(16.dp))
        when (tab) {
            0 -> GoalList(
                cards = inProgress,
                emptyMessage = "Everything in the catalog is complete. Nice work.",
            )
            1 -> GoalList(
                cards = done,
                emptyMessage = "Nothing completed yet. Keep rowing.",
            )
            else -> BadgeCase(badges = badges)
        }
    }
}

@Composable
private fun GoalList(
    cards: List<ChallengeCard>,
    emptyMessage: String,
) {
    if (cards.isEmpty()) {
        Text(emptyMessage, color = CatchTextSecondary)
        return
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(cards, key = { it.def.id }) { card ->
            GoalCardView(card = card)
        }
    }
}

@Composable
private fun GoalCardView(card: ChallengeCard) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), corner = 14.dp, contentPadding = 16.dp) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.def.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        kindLabel(card.def),
                        style = MaterialTheme.typography.labelMedium,
                        color = CatchAccent,
                    )
                }
                if (card.completed) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = "Completed",
                        tint = CatchGold,
                    )
                } else {
                    Text(
                        card.def.windowHint,
                        style = MaterialTheme.typography.labelMedium,
                        color = CatchTextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                card.def.blurb,
                style = MaterialTheme.typography.bodyMedium,
                color = CatchTextSecondary,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                progressLabel(card),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { card.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = CatchAccent,
                trackColor = CatchNavyElevated,
            )
            if (card.tierLabel != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (card.completed) "Complete · ${card.tierLabel}"
                    else "Current tier: ${card.tierLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = CatchGold,
                )
            }
            if (card.def.tiers.size > 1) {
                Spacer(Modifier.height(8.dp))
                Text(
                    card.def.tiers.joinToString(" · ") { tier ->
                        val mark = if (card.progressValue >= tier.threshold ||
                            (card.def.metric == ChallengeCatalog.Metric.FamilyMetersWeek &&
                                card.completed)
                        ) {
                            "✓"
                        } else {
                            "○"
                        }
                        "$mark ${tier.title}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = CatchTextSecondary,
                )
            }
        }
    }
}

@Composable
private fun BadgeCase(badges: List<BadgeAwardEntity>) {
    val earned = badges.map { it.badgeId }.toSet()
    val catalogBadges = buildList {
        addAll(BadgeRules.DISTANCE_MILESTONES)
        ChallengeCatalog.ALL.forEach { def ->
            def.tiers.forEach { tier ->
                add(BadgeRules.BadgeDef(tier.badgeId, tier.title, 0f))
            }
        }
    }.distinctBy { it.id }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(catalogBadges, key = { it.id }) { def ->
            val unlocked = def.id in earned
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CatchNavyElevated.copy(alpha = 0.6f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (unlocked) Icons.Outlined.EmojiEvents else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (unlocked) CatchGold else CatchTextSecondary,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) CatchGold.copy(alpha = 0.2f) else CatchNavy,
                        )
                        .padding(6.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(def.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (unlocked) "Earned" else "Locked",
                        style = MaterialTheme.typography.labelMedium,
                        color = CatchTextSecondary,
                    )
                }
            }
        }
    }
}

private fun kindLabel(def: ChallengeCatalog.Def): String {
    val kind = when (def.kind) {
        ChallengeCatalog.Kind.Challenge -> "Challenge"
        ChallengeCatalog.Kind.Program -> "Program"
    }
    val scope = when (def.scope) {
        ChallengeCatalog.Scope.Household -> "Family"
        ChallengeCatalog.Scope.Profile -> "Personal"
    }
    return "$kind · $scope"
}

private fun progressLabel(card: ChallengeCard): String {
    val p = card.progressValue
    val t = card.targetValue
    return when (card.def.metric) {
        ChallengeCatalog.Metric.FamilyMetersWeek,
        ChallengeCatalog.Metric.ProfileMetersMonth,
        ChallengeCatalog.Metric.LifetimeMeters,
        -> "${Formatters.meters(p)} / ${Formatters.meters(t)} m"
        ChallengeCatalog.Metric.WorkoutDaysWeek,
        ChallengeCatalog.Metric.DistinctWorkoutDays,
        -> "${p.toInt()} / ${t.toInt()} days"
        ChallengeCatalog.Metric.ScenicComplete ->
            if (p >= 1f) "Scenic complete" else "Scenic not finished yet"
    }
}
