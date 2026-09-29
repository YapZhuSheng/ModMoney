package com.modmoney.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.modmoney.data.Car
import com.modmoney.data.CategorySpend
import com.modmoney.data.GarageRepository
import com.modmoney.data.Mod
import com.modmoney.rules.CompatWarning
import com.modmoney.rules.CompatibilityRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Aggregated view state for one car's dashboard + mod list. */
data class CarDetailState(
    val car: Car? = null,
    val mods: List<Mod> = emptyList(),
    val categories: List<CategorySpend> = emptyList(),
    val totalSpent: Double = 0.0,
    val totalProjected: Double = 0.0,
    val loading: Boolean = true
) {
    val budgetCap: Double get() = car?.budgetCap ?: 0.0
    val remaining: Double get() = budgetCap - totalSpent
    val spentFraction: Float
        get() = if (budgetCap <= 0) 0f else (totalSpent / budgetCap).toFloat()
    val projectedFraction: Float
        get() = if (budgetCap <= 0) 0f else (totalProjected / budgetCap).toFloat()
    val isOverBudget: Boolean get() = budgetCap > 0 && totalSpent > budgetCap
    val isProjectedOver: Boolean get() = budgetCap > 0 && totalProjected > budgetCap

    /** Warnings for every mod, keyed by mod id. */
    val warningsByMod: Map<Long, List<CompatWarning>> by lazy {
        mods.associate { it.id to CompatibilityRules.evaluate(it, car, mods) }
    }

    val allWarnings: List<CompatWarning>
        get() = warningsByMod.values.flatten()
}

@OptIn(ExperimentalCoroutinesApi::class)
class GarageViewModel(private val repo: GarageRepository) : ViewModel() {

    val cars: StateFlow<List<Car>> = repo.observeCars()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val selectedCarId = MutableStateFlow<Long?>(null)

    /** Per-car aggregate state, recomputed whenever the selection or data changes. */
    val carDetail: StateFlow<CarDetailState> = selectedCarId
        .flatMapLatest { id ->
            if (id == null) flowOf(CarDetailState(loading = false))
            else detailFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CarDetailState())

    private fun detailFlow(carId: Long): Flow<CarDetailState> =
        combine(
            repo.observeCar(carId),
            repo.observeMods(carId),
            repo.observeCategorySpend(carId),
            repo.observeTotalSpent(carId),
            repo.observeTotalProjected(carId)
        ) { car, mods, cats, spent, projected ->
            CarDetailState(
                car = car,
                mods = mods,
                categories = cats,
                totalSpent = spent,
                totalProjected = projected,
                loading = false
            )
        }

    fun selectCar(carId: Long?) {
        selectedCarId.value = carId
    }

    /** Lightweight per-car totals for the garage list. */
    fun carSummary(carId: Long): Flow<Pair<Double, Double>> =
        combine(repo.observeTotalSpent(carId), repo.observeTotalProjected(carId)) { s, p -> s to p }

    fun modCount(carId: Long): Flow<Int> = repo.observeMods(carId).map { it.size }

    fun saveCar(car: Car, onSaved: (Long) -> Unit = {}) = viewModelScope.launch {
        onSaved(repo.saveCar(car))
    }

    fun deleteCar(carId: Long) = viewModelScope.launch {
        repo.deleteCar(carId)
        if (selectedCarId.value == carId) selectedCarId.value = null
    }

    fun saveMod(mod: Mod, onSaved: () -> Unit = {}) = viewModelScope.launch {
        repo.saveMod(mod)
        onSaved()
    }

    fun deleteMod(modId: Long) = viewModelScope.launch { repo.deleteMod(modId) }

    fun advanceStatus(mod: Mod) = viewModelScope.launch { repo.advanceStatus(mod) }

    /** Evaluates rules for a not-yet-saved mod so the editor can preview them. */
    fun previewWarnings(draft: Mod, car: Car?, siblings: List<Mod>): List<CompatWarning> =
        CompatibilityRules.evaluate(draft, car, siblings.filter { it.id != draft.id } + draft)

    class Factory(private val repo: GarageRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            GarageViewModel(repo) as T
    }
}
