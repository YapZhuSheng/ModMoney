package com.modmoney.data

import kotlinx.coroutines.flow.Flow

/**
 * Single access point for persistence. Thin by design — there is no network
 * layer and no caching, Room is the source of truth.
 */
class GarageRepository(
    private val carDao: CarDao,
    private val modDao: ModDao
) {
    fun observeCars(): Flow<List<Car>> = carDao.observeAll()
    fun observeCar(carId: Long): Flow<Car?> = carDao.observeById(carId)
    fun observeMods(carId: Long): Flow<List<Mod>> = modDao.observeForCar(carId)
    fun observeCategorySpend(carId: Long): Flow<List<CategorySpend>> =
        modDao.observeCategorySpend(carId)
    fun observeTotalSpent(carId: Long): Flow<Double> = modDao.observeTotalSpent(carId)
    fun observeTotalProjected(carId: Long): Flow<Double> = modDao.observeTotalProjected(carId)

    suspend fun saveCar(car: Car): Long =
        if (car.id == 0L) carDao.insert(car) else { carDao.update(car); car.id }

    suspend fun deleteCar(carId: Long) = carDao.deleteById(carId)

    suspend fun saveMod(mod: Mod): Long =
        if (mod.id == 0L) modDao.insert(mod) else { modDao.update(mod); mod.id }

    suspend fun deleteMod(modId: Long) = modDao.deleteById(modId)

    suspend fun findMod(modId: Long): Mod? = modDao.findById(modId)

    suspend fun modsForCar(carId: Long): List<Mod> = modDao.listForCar(carId)

    /** Cycles a mod through planned -> bought -> installed -> planned. */
    suspend fun advanceStatus(mod: Mod) {
        val next = when (mod.status) {
            ModStatus.PLANNED -> ModStatus.BOUGHT
            ModStatus.BOUGHT -> ModStatus.INSTALLED
            ModStatus.INSTALLED -> ModStatus.PLANNED
        }
        modDao.update(mod.copy(status = next))
    }
}
