package com.modmoney.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Aggregate row for the per-category dashboard breakdown. */
data class CategorySpend(
    val category: ModCategory,
    val spent: Double,
    val projected: Double,
    val count: Int
)

@Dao
interface CarDao {

    @Query("SELECT * FROM cars ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :carId")
    fun observeById(carId: Long): Flow<Car?>

    @Query("SELECT * FROM cars WHERE id = :carId")
    suspend fun findById(carId: Long): Car?

    @Query("SELECT COUNT(*) FROM cars")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(car: Car): Long

    @Update
    suspend fun update(car: Car)

    @Upsert
    suspend fun upsert(car: Car): Long

    @Delete
    suspend fun delete(car: Car)

    @Query("DELETE FROM cars WHERE id = :carId")
    suspend fun deleteById(carId: Long)
}

@Dao
interface ModDao {

    @Query("SELECT * FROM mods WHERE carId = :carId ORDER BY createdAt DESC")
    fun observeForCar(carId: Long): Flow<List<Mod>>

    @Query("SELECT * FROM mods WHERE id = :modId")
    suspend fun findById(modId: Long): Mod?

    @Query("SELECT * FROM mods WHERE carId = :carId")
    suspend fun listForCar(carId: Long): List<Mod>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mod: Mod): Long

    @Update
    suspend fun update(mod: Mod)

    @Upsert
    suspend fun upsert(mod: Mod): Long

    @Delete
    suspend fun delete(mod: Mod)

    @Query("DELETE FROM mods WHERE id = :modId")
    suspend fun deleteById(modId: Long)

    /**
     * Per-category totals computed in SQL.
     *
     * "spent" mirrors [Mod.spent]: bought/installed rows contribute their actual
     * price when set, otherwise their estimate. Planned rows contribute 0.
     * "projected" contributes actual-or-estimate for every row regardless of
     * status.
     */
    @Query(
        """
        SELECT category AS category,
               SUM(CASE WHEN status IN ('BOUGHT','INSTALLED')
                        THEN COALESCE(actualPrice, estimatedPrice) ELSE 0 END) AS spent,
               SUM(COALESCE(actualPrice, estimatedPrice)) AS projected,
               COUNT(*) AS count
        FROM mods
        WHERE carId = :carId
        GROUP BY category
        ORDER BY spent DESC, projected DESC
        """
    )
    fun observeCategorySpend(carId: Long): Flow<List<CategorySpend>>

    /** Total money already committed for a car. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN status IN ('BOUGHT','INSTALLED')
               THEN COALESCE(actualPrice, estimatedPrice) ELSE 0 END), 0)
        FROM mods WHERE carId = :carId
        """
    )
    fun observeTotalSpent(carId: Long): Flow<Double>

    /** Total projected cost if every listed mod is completed. */
    @Query(
        """
        SELECT COALESCE(SUM(COALESCE(actualPrice, estimatedPrice)), 0)
        FROM mods WHERE carId = :carId
        """
    )
    fun observeTotalProjected(carId: Long): Flow<Double>
}
