package com.modmoney.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A car in the garage.
 *
 * Stock reference figures (wheel diameter / tire width / aspect ratio) are stored
 * per car so the compatibility rule engine can compare a new wheel or tire
 * against what the car actually left the factory with. They are optional —
 * a null stock spec simply disables the rules that depend on it.
 */
@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val model: String,
    val year: Int,
    /** Budget ceiling for the whole build, in minor-unit-free whole currency. */
    val budgetCap: Double,
    @ColumnInfo(name = "stock_wheel_diameter_in") val stockWheelDiameterIn: Int? = null,
    @ColumnInfo(name = "stock_tire_width_mm") val stockTireWidthMm: Int? = null,
    @ColumnInfo(name = "stock_tire_aspect") val stockTireAspect: Int? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A single modification belonging to a car.
 *
 * Price model (documented decision): [estimatedPrice] is always required as the
 * planning figure. [actualPrice] is nullable and only meaningful once the part
 * is bought or installed; the dashboard falls back to the estimate when a
 * bought/installed item has no actual price recorded yet.
 */
@Entity(
    tableName = "mods",
    foreignKeys = [
        ForeignKey(
            entity = Car::class,
            parentColumns = ["id"],
            childColumns = ["carId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("carId")]
)
data class Mod(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val carId: Long,
    val name: String,
    val category: ModCategory,
    val estimatedPrice: Double,
    val actualPrice: Double? = null,
    val status: ModStatus = ModStatus.PLANNED,
    val priority: ModPriority = ModPriority.MEDIUM,
    /** Free-text compatibility notes written by the user. */
    val compatibilityNotes: String = "",

    // --- Optional structured specs that feed the rule engine ---
    /** Wheel diameter in inches, e.g. 17. */
    @ColumnInfo(name = "wheel_diameter_in") val wheelDiameterIn: Int? = null,
    /** Wheel width in inches, e.g. 7.5. */
    @ColumnInfo(name = "wheel_width_in") val wheelWidthIn: Double? = null,
    /** Wheel offset (ET) in mm, e.g. 40. */
    @ColumnInfo(name = "wheel_offset_mm") val wheelOffsetMm: Int? = null,
    /** Tire section width in mm, e.g. 205. */
    @ColumnInfo(name = "tire_width_mm") val tireWidthMm: Int? = null,
    /** Tire aspect ratio (profile) as a percentage, e.g. 45. */
    @ColumnInfo(name = "tire_aspect") val tireAspect: Int? = null,
    /** Suspension ride-height change in mm; negative lowers the car. */
    @ColumnInfo(name = "ride_height_delta_mm") val rideHeightDeltaMm: Int? = null,

    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Money actually committed for this mod. Bought/installed items use the
     * actual price when present, otherwise the estimate as a stand-in.
     */
    val spent: Double
        get() = if (status.isSpent) (actualPrice ?: estimatedPrice) else 0.0

    /** Best current guess of final cost, used for projected totals. */
    val projected: Double
        get() = actualPrice ?: estimatedPrice

    /** Positive when the part cost more than estimated. Null if unknown. */
    val variance: Double?
        get() = actualPrice?.let { it - estimatedPrice }
}
