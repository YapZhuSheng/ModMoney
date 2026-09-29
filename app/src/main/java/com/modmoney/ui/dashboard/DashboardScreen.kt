package com.modmoney.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modmoney.data.CategorySpend
import com.modmoney.data.ModStatus
import com.modmoney.data.WarningSeverity
import com.modmoney.ui.CarDetailState
import com.modmoney.ui.components.BudgetBar
import com.modmoney.ui.components.EmptyState
import com.modmoney.ui.components.StatBlock
import com.modmoney.ui.components.TagChip
import com.modmoney.ui.components.severityColor
import com.modmoney.util.Money

/**
 * Dashboard tab: budget headline, spend vs cap progress, status split,
 * per-category breakdown and a roll-up of compatibility warnings.
 */
@Composable
fun DashboardContent(
    state: CarDetailState,
    contentPadding: PaddingValues
) {
    if (state.car == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (!state.loading) EmptyState("Car not found", "It may have been deleted.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { BudgetCard(state) }

        if (state.mods.isNotEmpty()) {
            item { StatusSplitCard(state) }
            item { CategoryBreakdownCard(state.categories, state.totalSpent) }
        }

        val criticals = state.allWarnings.filter { it.severity != WarningSeverity.INFO }
        if (criticals.isNotEmpty()) {
            item { WarningRollupCard(state) }
        }

        if (state.mods.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "Nothing logged yet",
                    subtitle = "Switch to the Mods tab and add your first part to see " +
                        "the budget breakdown fill in."
                )
            }
        }

        state.car.notes.takeIf { it.isNotBlank() }?.let { notes ->
            item { NotesCard(notes) }
        }
    }
}

@Composable
private fun BudgetCard(state: CarDetailState) {
    val cap = state.budgetCap
    val over = state.isOverBudget
    val projectedOver = state.isProjectedOver

    val barColor = when {
        over -> MaterialTheme.colorScheme.error
        state.spentFraction >= 0.85f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "TOTAL SPENT",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Money.format(state.totalSpent),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (over) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "of ${Money.format(cap)} cap",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            BudgetBar(
                fraction = state.spentFraction,
                ghostFraction = state.projectedFraction,
                barColor = barColor,
                trackHeight = 12
            )
            Spacer(Modifier.height(7.dp))

            Row {
                Text(
                    text = "${Money.percent(
                        if (cap > 0) state.totalSpent / cap else 0.0
                    )} of budget used",
                    style = MaterialTheme.typography.bodySmall,
                    color = barColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "faded bar = projected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))

            Row {
                StatBlock(
                    label = if (over) "Over cap by" else "Remaining",
                    value = Money.format(
                        if (over) state.totalSpent - cap else state.remaining
                    ),
                    valueColor = if (over) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatBlock(
                    label = "Projected total",
                    value = Money.format(state.totalProjected),
                    valueColor = if (projectedOver) MaterialTheme.colorScheme.error
                                 else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            if (projectedOver && !over) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "You are under the cap now, but finishing every planned " +
                            "mod would put you " +
                            "${Money.format(state.totalProjected - cap)} over. " +
                            "Something has to give — trim scope, raise the cap, or " +
                            "drop a low-priority item.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusSplitCard(state: CarDetailState) {
    val byStatus = ModStatus.entries.associateWith { status ->
        state.mods.filter { it.status == status }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "BUILD PROGRESS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row {
                ModStatus.entries.forEach { status ->
                    val list = byStatus[status].orEmpty()
                    StatBlock(
                        label = status.label,
                        value = "${list.size}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModStatus.entries.forEach { status ->
                    val amount = byStatus[status].orEmpty().sumOf { it.projected }
                    if (amount > 0) {
                        TagChip(
                            text = "${status.label} ${Money.compact(amount)}",
                            color = when (status) {
                                ModStatus.PLANNED -> MaterialTheme.colorScheme.onSurfaceVariant
                                ModStatus.BOUGHT -> MaterialTheme.colorScheme.primary
                                ModStatus.INSTALLED -> MaterialTheme.colorScheme.tertiary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    categories: List<CategorySpend>,
    totalSpent: Double
) {
    if (categories.isEmpty()) return
    // Scale bars against the largest projected figure so every row is comparable.
    val maxProjected = categories.maxOf { it.projected }.coerceAtLeast(1.0)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BY CATEGORY",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "spent / projected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(14.dp))

            categories.forEachIndexed { index, row ->
                CategoryRow(row, maxProjected, totalSpent)
                if (index != categories.lastIndex) Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun CategoryRow(
    row: CategorySpend,
    maxProjected: Double,
    totalSpent: Double
) {
    val share = if (totalSpent > 0) row.spent / totalSpent else 0.0

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.category.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "·  ${row.count}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Money.format(row.spent),
                style = MaterialTheme.typography.bodyMedium,
                color = if (row.spent > 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = " / ${Money.format(row.projected)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        BudgetBar(
            fraction = (row.spent / maxProjected).toFloat(),
            ghostFraction = (row.projected / maxProjected).toFloat(),
            trackHeight = 7
        )
        if (row.spent > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${Money.percent(share)} of money spent so far",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun WarningRollupCard(state: CarDetailState) {
    // Group non-info warnings by the mod they came from.
    val entries = state.mods.mapNotNull { mod ->
        val warns = state.warningsByMod[mod.id].orEmpty()
            .filter { it.severity != WarningSeverity.INFO }
        if (warns.isEmpty()) null else mod to warns
    }
    if (entries.isEmpty()) return

    val criticalCount = entries.sumOf { (_, w) ->
        w.count { it.severity == WarningSeverity.CRITICAL }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (criticalCount > 0) Icons.Default.ErrorOutline
                                  else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = if (criticalCount > 0) MaterialTheme.colorScheme.error
                           else severityColor(WarningSeverity.CAUTION),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "THINGS TO CHECK",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))

            entries.forEachIndexed { index, (mod, warns) ->
                Column {
                    Text(
                        text = mod.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(5.dp))
                    warns.forEach { w ->
                        Row(Modifier.padding(bottom = 4.dp)) {
                            Box(
                                Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .background(
                                        color = severityColor(w.severity),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = w.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (index != entries.lastIndex) Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "Open a mod on the Mods tab for the full explanation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "BUILD NOTES",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
