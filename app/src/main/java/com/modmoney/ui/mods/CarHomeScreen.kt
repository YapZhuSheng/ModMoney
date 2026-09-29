package com.modmoney.ui.mods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.modmoney.data.Mod
import com.modmoney.data.ModStatus
import com.modmoney.data.WarningSeverity
import com.modmoney.rules.CompatWarning
import com.modmoney.ui.CarDetailState
import com.modmoney.ui.GarageViewModel
import com.modmoney.ui.components.EmptyState
import com.modmoney.ui.components.PriorityChip
import com.modmoney.ui.components.StatusChip
import com.modmoney.ui.components.TagChip
import com.modmoney.ui.components.WarningCard
import com.modmoney.ui.components.severityColor
import com.modmoney.ui.dashboard.DashboardContent
import com.modmoney.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarHomeScreen(
    vm: GarageViewModel,
    carId: Long,
    onBack: () -> Unit,
    onAddMod: () -> Unit,
    onEditMod: (Long) -> Unit,
    onEditCar: () -> Unit
) {
    val state by vm.carDetail.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Mods")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                state.car?.name ?: "Loading…",
                                style = MaterialTheme.typography.titleLarge
                            )
                            state.car?.let {
                                Text(
                                    "${it.year} · ${it.model}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to garage"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onEditCar) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit car")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = {
                                Text(
                                    if (index == 1 && state.mods.isNotEmpty())
                                        "$title (${state.mods.size})" else title
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMod,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add mod") }
            )
        }
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 96.dp
        )

        when (tab) {
            0 -> DashboardContent(state = state, contentPadding = contentPadding)
            else -> ModList(
                state = state,
                contentPadding = contentPadding,
                onEditMod = onEditMod,
                onAdvance = vm::advanceStatus
            )
        }
    }
}

@Composable
private fun ModList(
    state: CarDetailState,
    contentPadding: PaddingValues,
    onEditMod: (Long) -> Unit,
    onAdvance: (Mod) -> Unit
) {
    if (state.mods.isEmpty() && !state.loading) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            EmptyState(
                title = "No mods logged",
                subtitle = "Add a mod with its category, estimated price and status. " +
                    "Fill in wheel or tire specs and ModMoney will check them for you."
            )
        }
        return
    }

    // Sort: priority first, then unfinished before finished, then newest.
    val sorted = remember(state.mods) {
        state.mods.sortedWith(
            compareBy(
                { it.priority.sortWeight },
                { it.status.ordinal },
                { -it.createdAt }
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(sorted, key = { it.id }) { mod ->
            ModRow(
                mod = mod,
                warnings = state.warningsByMod[mod.id].orEmpty(),
                onClick = { onEditMod(mod.id) },
                onAdvance = { onAdvance(mod) }
            )
        }
    }
}

@Composable
private fun ModRow(
    mod: Mod,
    warnings: List<CompatWarning>,
    onClick: () -> Unit,
    onAdvance: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val peak = warnings.maxByOrNull { it.severity.ordinal }?.severity

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(start = 14.dp, end = 6.dp, top = 13.dp, bottom = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = mod.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = mod.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusChip(mod.status)
                        PriorityChip(mod.priority)
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Money.format(mod.projected),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (mod.status.isSpent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mod.actualPrice != null && mod.variance != null &&
                        mod.variance != 0.0
                    ) {
                        val v = mod.variance!!
                        Text(
                            text = "${Money.signed(v)} vs est.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (v > 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.tertiary
                        )
                    } else if (mod.actualPrice == null) {
                        Text(
                            text = "estimate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Compatibility summary strip
            if (warnings.isNotEmpty() && peak != null) {
                val color = severityColor(peak)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (peak) {
                            WarningSeverity.INFO -> Icons.Default.Info
                            WarningSeverity.CAUTION -> Icons.Default.WarningAmber
                            WarningSeverity.CRITICAL -> Icons.Default.ErrorOutline
                        },
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (expanded) "Hide compatibility notes"
                        else "${warnings.size} compatibility " +
                            if (warnings.size == 1) "note" else "notes",
                        style = MaterialTheme.typography.labelLarge,
                        color = color
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(
                            start = 14.dp, end = 14.dp, bottom = 14.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (mod.compatibilityNotes.isNotBlank()) {
                            Column {
                                Text(
                                    "YOUR NOTES",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    mod.compatibilityNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        warnings.forEach { WarningCard(it) }
                        SpecSummary(mod)
                    }
                }
            } else if (mod.compatibilityNotes.isNotBlank()) {
                Text(
                    text = mod.compatibilityNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 13.dp)
                )
            }
        }
    }
}

/** Chips describing whatever structured specs the mod carries. */
@Composable
private fun SpecSummary(mod: Mod) {
    val chips = buildList {
        mod.wheelDiameterIn?.let { add("${it}\" rim") }
        mod.wheelWidthIn?.let {
            add("${if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()}J width")
        }
        mod.wheelOffsetMm?.let { add("ET$it") }
        if (mod.tireWidthMm != null && mod.tireAspect != null) {
            add("${mod.tireWidthMm}/${mod.tireAspect}")
        } else {
            mod.tireWidthMm?.let { add("$it mm wide") }
            mod.tireAspect?.let { add("$it profile") }
        }
        mod.rideHeightDeltaMm?.let {
            add(if (it < 0) "${-it} mm lower" else "$it mm higher")
        }
    }
    if (chips.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        chips.take(4).forEach {
            TagChip(text = it, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
