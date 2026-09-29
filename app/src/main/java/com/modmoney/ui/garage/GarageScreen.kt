package com.modmoney.ui.garage

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modmoney.data.Car
import com.modmoney.ui.GarageViewModel
import com.modmoney.ui.components.BudgetBar
import com.modmoney.ui.components.EmptyState
import com.modmoney.ui.components.TagChip
import com.modmoney.util.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    vm: GarageViewModel,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAddCar: () -> Unit,
    onOpenCar: (Long) -> Unit,
    onEditCar: (Long) -> Unit
) {
    val cars by vm.cars.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ModMoney", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Garage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (darkTheme) Icons.Default.LightMode
                                          else Icons.Default.DarkMode,
                            contentDescription = if (darkTheme) "Switch to light theme"
                                                 else "Switch to dark theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCar,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add car") }
            )
        }
    ) { padding ->
        if (cars.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "No cars yet",
                    subtitle = "Add your first car with a model, year and budget cap to start tracking mods."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(cars, key = { it.id }) { car ->
                    CarCard(
                        car = car,
                        vm = vm,
                        onClick = { onOpenCar(car.id) },
                        onEdit = { onEditCar(car.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CarCard(
    car: Car,
    vm: GarageViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val summaryFlow = remember(car.id) { vm.carSummary(car.id) }
    val summary by summaryFlow.collectAsState(initial = 0.0 to 0.0)
    val countFlow = remember(car.id) { vm.modCount(car.id) }
    val modCount by countFlow.collectAsState(initial = 0)

    val spent = summary.first
    val projected = summary.second
    val cap = car.budgetCap
    val fraction = if (cap > 0) (spent / cap).toFloat() else 0f
    val projFraction = if (cap > 0) (projected / cap).toFloat() else 0f
    val over = cap > 0 && spent > cap

    val barColor = when {
        over -> MaterialTheme.colorScheme.error
        fraction >= 0.85f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = car.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${car.year} · ${car.model}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit ${car.name}",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = Money.format(spent),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (over) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "of ${Money.format(cap)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = Money.percent(if (cap > 0) spent / cap else 0.0),
                    style = MaterialTheme.typography.labelLarge,
                    color = barColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))
            BudgetBar(
                fraction = fraction,
                ghostFraction = projFraction,
                barColor = barColor
            )
            Spacer(Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagChip(
                    text = "$modCount ${if (modCount == 1) "mod" else "mods"}",
                    color = MaterialTheme.colorScheme.secondary
                )
                if (cap > 0 && projected > cap) {
                    TagChip(
                        text = "Projected ${Money.compact(projected)}",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (spent <= cap) {
                    TagChip(
                        text = "${Money.compact(cap - spent)} left",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
