package com.modmoney.ui.garage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.modmoney.data.Car
import com.modmoney.ui.GarageViewModel
import com.modmoney.ui.components.StatBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarEditorScreen(
    vm: GarageViewModel,
    carId: Long,
    onDone: () -> Unit
) {
    val cars by vm.cars.collectAsState()
    val existing = remember(cars, carId) { cars.firstOrNull { it.id == carId } }
    val isNew = carId == 0L

    var name by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    var stockDia by remember { mutableStateOf("") }
    var stockWidth by remember { mutableStateOf("") }
    var stockAspect by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var hydrated by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // Populate fields once the row arrives from the database.
    LaunchedEffect(existing) {
        if (existing != null && !hydrated) {
            name = existing.name
            model = existing.model
            year = existing.year.toString()
            budget = trimNumber(existing.budgetCap)
            stockDia = existing.stockWheelDiameterIn?.toString() ?: ""
            stockWidth = existing.stockTireWidthMm?.toString() ?: ""
            stockAspect = existing.stockTireAspect?.toString() ?: ""
            notes = existing.notes
            hydrated = true
        }
    }

    val nameOk = name.isNotBlank()
    val modelOk = model.isNotBlank()
    val yearValue = year.toIntOrNull()
    val yearOk = yearValue != null && yearValue in 1900..2100
    val budgetValue = budget.toDoubleOrNull()
    val budgetOk = budgetValue != null && budgetValue > 0
    val canSave = nameOk && modelOk && yearOk && budgetOk

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add car" else "Edit car") },
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
                                contentDescription = "Delete car",
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
                label = { Text("Nickname") },
                placeholder = { Text("Daily City") },
                singleLine = true,
                isError = name.isNotEmpty() && !nameOk,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                placeholder = { Text("Honda City VX CVT") },
                singleLine = true,
                isError = model.isNotEmpty() && !modelOk,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    label = { Text("Year") },
                    singleLine = true,
                    isError = year.isNotEmpty() && !yearOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget cap") },
                    singleLine = true,
                    isError = budget.isNotEmpty() && !budgetOk,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1.4f)
                )
            }

            Spacer(Modifier.height(4.dp))
            SectionLabel("Stock reference (optional)")
            Text(
                "Used by the compatibility checks to compare new wheels and tires " +
                    "against what the car came with. Leave blank to skip those warnings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = stockDia,
                    onValueChange = { stockDia = it.filter(Char::isDigit).take(2) },
                    label = { Text("Rim \"") },
                    placeholder = { Text("16") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = stockWidth,
                    onValueChange = { stockWidth = it.filter(Char::isDigit).take(3) },
                    label = { Text("Tire mm") },
                    placeholder = { Text("185") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = stockAspect,
                    onValueChange = { stockAspect = it.filter(Char::isDigit).take(2) },
                    label = { Text("Profile") },
                    placeholder = { Text("55") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            if (stockDia.isNotBlank() && stockWidth.isNotBlank() && stockAspect.isNotBlank()) {
                StatBlock(
                    label = "Stock size reads as",
                    value = "$stockWidth/$stockAspect R$stockDia"
                )
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Build notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val car = Car(
                        id = existing?.id ?: 0L,
                        name = name.trim(),
                        model = model.trim(),
                        year = yearValue ?: 0,
                        budgetCap = budgetValue ?: 0.0,
                        stockWheelDiameterIn = stockDia.toIntOrNull(),
                        stockTireWidthMm = stockWidth.toIntOrNull(),
                        stockTireAspect = stockAspect.toIntOrNull(),
                        notes = notes.trim(),
                        createdAt = existing?.createdAt ?: System.currentTimeMillis()
                    )
                    vm.saveCar(car) { onDone() }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNew) "Add to garage" else "Save changes")
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${existing.name}?") },
            text = {
                Text(
                    "This removes the car and every mod recorded against it. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteCar(existing.id)
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
internal fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}

/** Renders 180000.0 as "180000" so editing does not show a stray ".0". */
internal fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
