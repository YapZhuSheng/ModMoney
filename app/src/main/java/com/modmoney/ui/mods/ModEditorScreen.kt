package com.modmoney.ui.mods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.modmoney.data.Mod
import com.modmoney.data.ModCategory
import com.modmoney.data.ModPriority
import com.modmoney.data.ModStatus
import com.modmoney.ui.GarageViewModel
import com.modmoney.ui.components.WarningCard
import com.modmoney.util.Money

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ModEditorScreen(
    vm: GarageViewModel,
    carId: Long,
    modId: Long,
    onDone: () -> Unit
) {
    val state by vm.carDetail.collectAsState()
    val existing = remember(state.mods, modId) { state.mods.firstOrNull { it.id == modId } }
    val isNew = modId == 0L

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ModCategory.OTHER) }
    var estimated by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ModStatus.PLANNED) }
    var priority by remember { mutableStateOf(ModPriority.MEDIUM) }
    var notes by remember { mutableStateOf("") }

    var wheelDia by remember { mutableStateOf("") }
    var wheelWidth by remember { mutableStateOf("") }
    var offset by remember { mutableStateOf("") }
    var tireWidth by remember { mutableStateOf("") }
    var tireAspect by remember { mutableStateOf("") }
    var drop by remember { mutableStateOf("") }

    var hydrated by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        if (existing != null && !hydrated) {
            name = existing.name
            category = existing.category
            estimated = trimNum(existing.estimatedPrice)
            actual = existing.actualPrice?.let { trimNum(it) } ?: ""
            status = existing.status
            priority = existing.priority
            notes = existing.compatibilityNotes
            wheelDia = existing.wheelDiameterIn?.toString() ?: ""
            wheelWidth = existing.wheelWidthIn?.let { trimNum(it) } ?: ""
            offset = existing.wheelOffsetMm?.toString() ?: ""
            tireWidth = existing.tireWidthMm?.toString() ?: ""
            tireAspect = existing.tireAspect?.toString() ?: ""
            drop = existing.rideHeightDeltaMm?.toString() ?: ""
            hydrated = true
        }
    }

    val nameOk = name.isNotBlank()
    val estValue = estimated.toDoubleOrNull()
    val estOk = estValue != null && estValue >= 0
    val actValue = actual.toDoubleOrNull()
    val actOk = actual.isBlank() || (actValue != null && actValue >= 0)
    val canSave = nameOk && estOk && actOk

    // Live draft used purely to preview rule output while typing.
    val draft = Mod(
        id = existing?.id ?: 0L,
        carId = carId,
        name = name.ifBlank { "This mod" },
        category = category,
        estimatedPrice = estValue ?: 0.0,
        actualPrice = actValue,
        status = status,
        priority = priority,
        compatibilityNotes = notes,
        wheelDiameterIn = wheelDia.toIntOrNull(),
        wheelWidthIn = wheelWidth.toDoubleOrNull(),
        wheelOffsetMm = offset.toIntOrNull(),
        tireWidthMm = tireWidth.toIntOrNull(),
        tireAspect = tireAspect.toIntOrNull(),
        rideHeightDeltaMm = drop.toIntOrNull()
    )
    val warnings = remember(draft, state.car, state.mods) {
        vm.previewWarnings(draft, state.car, state.mods)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add mod" else "Edit mod") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete mod",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Mod name") },
                placeholder = { Text("17x7.5 ET42 alloys") },
                singleLine = true,
                isError = name.isNotEmpty() && !nameOk,
                modifier = Modifier.fillMaxWidth()
            )

            FieldLabel("Category")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                ModCategory.entries.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = estimated,
                    onValueChange = { estimated = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Estimated price") },
                    singleLine = true,
                    isError = estimated.isNotEmpty() && !estOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = actual,
                    onValueChange = { actual = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Actual price") },
                    placeholder = { Text("optional") },
                    singleLine = true,
                    isError = !actOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // Variance readout
            if (estValue != null && actValue != null && estValue > 0) {
                val variance = actValue - estValue
                if (variance != 0.0) {
                    Text(
                        text = if (variance > 0)
                            "${Money.signed(variance)} over your estimate"
                        else
                            "${Money.signed(variance)} under your estimate",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (variance > 0) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            FieldLabel("Status")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ModStatus.entries.forEach { s ->
                    FilterChip(
                        selected = status == s,
                        onClick = { status = s },
                        label = { Text(s.label) }
                    )
                }
            }
            if (status.isSpent && actual.isBlank()) {
                Text(
                    text = "No actual price yet, so the estimate is counted as spent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FieldLabel("Priority")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ModPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) }
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            FieldLabel("Specs (optional — powers the compatibility checks)")

            if (category == ModCategory.WHEELS || wheelDia.isNotBlank() ||
                wheelWidth.isNotBlank() || offset.isNotBlank()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = wheelDia,
                        onValueChange = { wheelDia = it.filter(Char::isDigit).take(2) },
                        label = { Text("Rim \"") },
                        placeholder = { Text("17") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = wheelWidth,
                        onValueChange = {
                            wheelWidth = it.filter { c -> c.isDigit() || c == '.' }.take(4)
                        },
                        label = { Text("Width J") },
                        placeholder = { Text("7.5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = offset,
                        onValueChange = { offset = it.filter(Char::isDigit).take(3) },
                        label = { Text("ET") },
                        placeholder = { Text("42") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (category == ModCategory.TIRES || category == ModCategory.WHEELS ||
                tireWidth.isNotBlank() || tireAspect.isNotBlank()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = tireWidth,
                        onValueChange = { tireWidth = it.filter(Char::isDigit).take(3) },
                        label = { Text("Tire width mm") },
                        placeholder = { Text("205") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tireAspect,
                        onValueChange = { tireAspect = it.filter(Char::isDigit).take(2) },
                        label = { Text("Profile") },
                        placeholder = { Text("45") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (category == ModCategory.SUSPENSION || drop.isNotBlank()) {
                OutlinedTextField(
                    value = drop,
                    onValueChange = {
                        drop = it.filter { c -> c.isDigit() || c == '-' }.take(4)
                    },
                    label = { Text("Ride height change (mm)") },
                    placeholder = { Text("-35 to lower, 20 to raise") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Compatibility notes") },
                placeholder = { Text("Fitment details, clearances, anything to verify…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Live rule preview
            AnimatedVisibility(visible = warnings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel(
                        "Compatibility check · ${warnings.size} " +
                            if (warnings.size == 1) "note" else "notes"
                    )
                    warnings.forEach { WarningCard(it) }
                    Text(
                        text = "These are automatic heuristics for a street build, not a " +
                            "fitment guarantee. Verify anything load-bearing or legal " +
                            "with your installer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val mod = Mod(
                        id = existing?.id ?: 0L,
                        carId = carId,
                        name = name.trim(),
                        category = category,
                        estimatedPrice = estValue ?: 0.0,
                        actualPrice = actValue,
                        status = status,
                        priority = priority,
                        compatibilityNotes = notes.trim(),
                        wheelDiameterIn = wheelDia.toIntOrNull(),
                        wheelWidthIn = wheelWidth.toDoubleOrNull(),
                        wheelOffsetMm = offset.toIntOrNull(),
                        tireWidthMm = tireWidth.toIntOrNull(),
                        tireAspect = tireAspect.toIntOrNull(),
                        rideHeightDeltaMm = drop.toIntOrNull(),
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    vm.saveMod(mod) { onDone() }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNew) "Add mod" else "Save changes")
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this mod?") },
            text = { Text("\"${existing.name}\" will be removed from the build.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteMod(existing.id)
                    onDone()
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}

private fun trimNum(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
