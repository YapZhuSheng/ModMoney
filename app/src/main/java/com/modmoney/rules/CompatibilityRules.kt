package com.modmoney.rules

import com.modmoney.data.Car
import com.modmoney.data.Mod
import com.modmoney.data.ModCategory
import com.modmoney.data.WarningSeverity
import kotlin.math.abs
import kotlin.math.roundToInt

/** One generated advisory about a mod. */
data class CompatWarning(
    val title: String,
    val detail: String,
    val severity: WarningSeverity,
    /** Stable id so the UI can key list items. */
    val ruleId: String
)

/**
 * A deliberately simple, fully offline rule engine.
 *
 * Design notes / documented decisions:
 *  - Rules are heuristics for a street build, not engineering certification.
 *    Every message is phrased as something to check, never as a hard verdict.
 *  - Rules only fire when the data they need is present. A mod with no
 *    structured specs produces no warnings, so the feature never nags.
 *  - Thresholds are constants in one place so they are easy to audit and tune.
 */
object CompatibilityRules {

    // Tolerances
    private const val ROLLING_DIA_GOOD_PCT = 3.0    // within this = effectively stock
    private const val ROLLING_DIA_WARN_PCT = 5.0    // beyond this = speedo/gearing concern
    private const val LOW_PROFILE_ASPECT = 45       // at/below this, ride quality suffers
    private const val VERY_LOW_PROFILE_ASPECT = 35
    private const val OFFSET_DELTA_WARN_MM = 12     // ET change that starts to matter
    private const val BIG_DROP_MM = -40             // beyond this, geometry work needed
    private const val MILD_DROP_MM = -25

    /** Inches -> mm. */
    private fun inToMm(inches: Double) = inches * 25.4

    /**
     * Overall rolling diameter of a wheel+tire combo, in mm.
     * diameter = rim + 2 * sidewall, where sidewall = width * aspect/100.
     */
    private fun rollingDiameterMm(rimIn: Int, widthMm: Int, aspect: Int): Double =
        inToMm(rimIn.toDouble()) + 2.0 * (widthMm * aspect / 100.0)

    /**
     * Evaluates every rule for [mod] in the context of [car] and its sibling
     * [allMods] (siblings let wheel and tire entries inform each other).
     */
    fun evaluate(mod: Mod, car: Car?, allMods: List<Mod> = emptyList()): List<CompatWarning> {
        val out = mutableListOf<CompatWarning>()

        wheelDiameterRule(mod, car, out)
        rollingDiameterRule(mod, car, allMods, out)
        lowProfileRule(mod, out)
        offsetRule(mod, car, out)
        widthRule(mod, car, out)
        rideHeightRule(mod, out)
        loweredPlusBigWheelRule(mod, car, allMods, out)
        wheelWithoutTireRule(mod, allMods, out)
        brakeClearanceRule(mod, car, allMods, out)

        return out.sortedByDescending { it.severity.ordinal }
    }

    // ---- Rule 1: wheel larger than stock diameter -------------------------

    private fun wheelDiameterRule(mod: Mod, car: Car?, out: MutableList<CompatWarning>) {
        val newDia = mod.wheelDiameterIn ?: return
        val stockDia = car?.stockWheelDiameterIn ?: return
        val step = newDia - stockDia
        if (step <= 0) return

        out += CompatWarning(
            ruleId = "wheel_diameter_up",
            severity = if (step >= 2) WarningSeverity.CRITICAL else WarningSeverity.CAUTION,
            title = "+$step\" over stock wheel diameter ($stockDia\" → $newDia\")",
            detail = buildString {
                append("Going up in rim size without dropping tire profile raises the ")
                append("overall rolling diameter, which makes the speedometer read low ")
                append("(you will actually be travelling faster than indicated) and ")
                append("adds error to the odometer. ")
                val stockW = car.stockTireWidthMm
                val stockA = car.stockTireAspect
                if (stockW != null && stockA != null) {
                    val target = suggestAspect(stockW, stockA, car.stockWheelDiameterIn, newDia, stockW)
                    if (target != null) {
                        append("To stay near stock rolling diameter on a $newDia\" rim, ")
                        append("aim for roughly a $target-series tire ")
                        append("(about $stockW/$target R$newDia). ")
                    }
                }
                append("Also budget for the ride-quality loss from the shorter sidewall, ")
                append("and re-check clearance at full steering lock.")
            }
        )
    }

    /**
     * Finds the aspect ratio that best preserves rolling diameter when moving
     * from [fromRim] to [toRim], holding section width at [newWidthMm].
     * Returns a value rounded to the nearest 5, or null if nothing sane fits.
     */
    private fun suggestAspect(
        stockWidthMm: Int,
        stockAspect: Int,
        fromRim: Int,
        toRim: Int,
        newWidthMm: Int
    ): Int? {
        val targetDia = rollingDiameterMm(fromRim, stockWidthMm, stockAspect)
        val sidewall = (targetDia - inToMm(toRim.toDouble())) / 2.0
        val aspect = (sidewall / newWidthMm * 100.0)
        val rounded = (aspect / 5.0).roundToInt() * 5
        return rounded.takeIf { it in 25..80 }
    }

    // ---- Rule 2: rolling diameter / speedometer offset --------------------

    private fun rollingDiameterRule(
        mod: Mod,
        car: Car?,
        allMods: List<Mod>,
        out: MutableList<CompatWarning>
    ) {
        val stockW = car?.stockTireWidthMm ?: return
        val stockA = car.stockTireAspect ?: return
        val stockRim = car.stockWheelDiameterIn ?: return

        // Resolve the new combo: this mod may carry the tire spec, the wheel
        // spec, or both. Fall back to a sibling entry for the missing half.
        val rim = mod.wheelDiameterIn
            ?: allMods.firstOrNull { it.id != mod.id && it.wheelDiameterIn != null }?.wheelDiameterIn
            ?: stockRim
        val width = mod.tireWidthMm
            ?: allMods.firstOrNull { it.id != mod.id && it.tireWidthMm != null }?.tireWidthMm
            ?: return
        val aspect = mod.tireAspect
            ?: allMods.firstOrNull { it.id != mod.id && it.tireAspect != null }?.tireAspect
            ?: return

        // Only report from the entry that actually holds tire data, to avoid
        // duplicating the same warning on both the wheel and the tire row.
        if (mod.tireWidthMm == null && mod.tireAspect == null) return

        val stockDia = rollingDiameterMm(stockRim, stockW, stockA)
        val newDia = rollingDiameterMm(rim, width, aspect)
        val deltaMm = newDia - stockDia
        val deltaPct = deltaMm / stockDia * 100.0
        if (abs(deltaPct) < 0.1) return

        val indicatedAt100 = 100.0 * newDia / stockDia
        val pctStr = String.format("%.1f", abs(deltaPct))
        val mmStr = String.format("%.1f", abs(deltaMm))
        val trueSpeed = String.format("%.1f", indicatedAt100)

        val severity = when {
            abs(deltaPct) <= ROLLING_DIA_GOOD_PCT -> WarningSeverity.INFO
            abs(deltaPct) <= ROLLING_DIA_WARN_PCT -> WarningSeverity.CAUTION
            else -> WarningSeverity.CRITICAL
        }

        val direction = if (deltaMm > 0) "larger" else "smaller"
        val readsHow = if (deltaMm > 0) "low" else "high"

        out += CompatWarning(
            ruleId = "rolling_diameter",
            severity = severity,
            title = if (severity == WarningSeverity.INFO)
                "Rolling diameter within $pctStr% of stock — good match"
            else
                "Rolling diameter $pctStr% $direction than stock",
            detail = buildString {
                append("New setup: $width/$aspect R$rim. ")
                append("Overall diameter changes by $mmStr mm ($pctStr%) versus the ")
                append("stock $stockW/$stockA R$stockRim. ")
                if (severity == WarningSeverity.INFO) {
                    append("That is inside the ±${ROLLING_DIA_GOOD_PCT.toInt()}% window most ")
                    append("people treat as stock-equivalent, so speedometer error stays ")
                    append("negligible. ")
                } else {
                    append("The speedometer will read $readsHow: at an indicated 100 km/h ")
                    append("you are actually doing about $trueSpeed km/h. ")
                    append("Expect a matching odometer error and a slight change in ")
                    append("effective gearing")
                    if (deltaMm > 0) append(" (taller overall, marginally slower off the line)")
                    else append(" (shorter overall, marginally quicker but higher revs at cruise)")
                    append(". ")
                }
                append("Tire profile note: sidewall height is now about ")
                append("${(width * aspect / 100.0).roundToInt()} mm.")
            }
        )
    }

    // ---- Rule 3: low-profile ride quality --------------------------------

    private fun lowProfileRule(mod: Mod, out: MutableList<CompatWarning>) {
        val aspect = mod.tireAspect ?: return
        if (aspect > LOW_PROFILE_ASPECT) return

        val veryLow = aspect <= VERY_LOW_PROFILE_ASPECT
        out += CompatWarning(
            ruleId = "low_profile",
            severity = if (veryLow) WarningSeverity.CAUTION else WarningSeverity.INFO,
            title = "$aspect-series tire is low profile",
            detail = buildString {
                append("A $aspect aspect ratio leaves a short sidewall, so the tire ")
                append("absorbs less of the road. ")
                if (veryLow) {
                    append("At this profile, pothole impacts are a real risk to both the ")
                    append("rim and the sidewall, and ride harshness is noticeable on ")
                    append("poor surfaces. ")
                }
                append("Keep pressures to spec — under-inflation on a low-profile tire ")
                append("concentrates damage on the rim lip.")
            }
        )
    }

    // ---- Rule 4: offset (ET) change --------------------------------------

    private fun offsetRule(mod: Mod, car: Car?, out: MutableList<CompatWarning>) {
        val et = mod.wheelOffsetMm ?: return
        // Typical FWD hatch/sedan factory offset when unknown for this car.
        val assumedStockEt = 45
        val delta = et - assumedStockEt
        if (abs(delta) < OFFSET_DELTA_WARN_MM) return

        val outward = delta < 0
        out += CompatWarning(
            ruleId = "offset_change",
            severity = WarningSeverity.CAUTION,
            title = "ET$et sits ${abs(delta)} mm further ${if (outward) "out" else "in"} than typical stock",
            detail = buildString {
                append("Offset moves the wheel ")
                if (outward) {
                    append("outward toward the fender. Check for guard rub at full ")
                    append("compression and full lock, and expect slightly heavier ")
                    append("steering plus more load on wheel bearings. ")
                } else {
                    append("inward toward the strut and brake caliper. Check caliper ")
                    append("and strut clearance before committing. ")
                }
                append("This is measured against an assumed ET$assumedStockEt stock ")
                append("figure — confirm your car's actual offset.")
            }
        )
    }

    // ---- Rule 5: section width vs stock ----------------------------------

    private fun widthRule(mod: Mod, car: Car?, out: MutableList<CompatWarning>) {
        val newW = mod.tireWidthMm ?: return
        val stockW = car?.stockTireWidthMm ?: return
        val delta = newW - stockW
        if (delta < 15) return

        out += CompatWarning(
            ruleId = "tire_width_up",
            severity = if (delta >= 30) WarningSeverity.CAUTION else WarningSeverity.INFO,
            title = "Tire is $delta mm wider than stock ($stockW → $newW mm)",
            detail = buildString {
                append("Wider rubber adds grip but also adds tramlining, a little more ")
                append("steering effort, and a small fuel-economy penalty. ")
                append("Make sure the rim width suits a $newW mm section")
                mod.wheelWidthIn?.let { append(" (this wheel is ${trimWidth(it)}\" wide)") }
                append(", and re-check inner-guard clearance at full lock.")
            }
        )
    }

    private fun trimWidth(w: Double): String =
        if (w % 1.0 == 0.0) w.toInt().toString() else w.toString()

    // ---- Rule 6: ride height -------------------------------------------

    private fun rideHeightRule(mod: Mod, out: MutableList<CompatWarning>) {
        val drop = mod.rideHeightDeltaMm ?: return
        if (drop >= 0) {
            if (drop > 0) {
                out += CompatWarning(
                    ruleId = "raised",
                    severity = WarningSeverity.INFO,
                    title = "Ride height raised by $drop mm",
                    detail = "Raising the car changes headlight aim and raises the centre " +
                        "of gravity. Re-aim headlights after fitting."
                )
            }
            return
        }

        val amount = abs(drop)
        val big = drop <= BIG_DROP_MM
        out += CompatWarning(
            ruleId = "lowered",
            severity = if (big) WarningSeverity.CAUTION else WarningSeverity.INFO,
            title = "Lowered $amount mm",
            detail = buildString {
                append("Dropping $amount mm reduces suspension travel and steepens ")
                append("camber, which wears the inner shoulder of the tires faster. ")
                if (big) {
                    append("Past about ${abs(BIG_DROP_MM)} mm you should plan a proper ")
                    append("alignment and consider camber correction; bump-stop contact ")
                    append("and driveway scraping become likely. ")
                } else if (drop <= MILD_DROP_MM) {
                    append("Get an alignment afterwards. ")
                }
                append("Headlight aim also drops — re-aim after fitting.")
            }
        )
    }

    // ---- Rule 7: lowered + oversized wheel interaction -------------------

    private fun loweredPlusBigWheelRule(
        mod: Mod,
        car: Car?,
        allMods: List<Mod>,
        out: MutableList<CompatWarning>
    ) {
        val stockDia = car?.stockWheelDiameterIn ?: return
        val drop = mod.rideHeightDeltaMm ?: return
        if (drop >= 0) return

        val bigWheel = allMods.firstOrNull {
            it.id != mod.id && (it.wheelDiameterIn ?: 0) > stockDia
        } ?: return

        out += CompatWarning(
            ruleId = "lowered_plus_big_wheel",
            severity = WarningSeverity.CAUTION,
            title = "Lowering ${abs(drop)} mm on ${bigWheel.wheelDiameterIn}\" wheels — check guard clearance",
            detail = buildString {
                append("You have \"${bigWheel.name}\" in this build at ")
                append("${bigWheel.wheelDiameterIn}\" (stock is $stockDia\"), and this entry ")
                append("lowers the car ${abs(drop)} mm. The combination shrinks the gap ")
                append("between tire and guard from both directions at once, so rubbing ")
                append("on full compression is the most likely problem. ")
                append("Roll the guards or verify clearance with the car fully loaded ")
                append("before signing off on either part.")
            }
        )
    }

    // ---- Rule 8: wheels without matching tire entry ----------------------

    private fun wheelWithoutTireRule(
        mod: Mod,
        allMods: List<Mod>,
        out: MutableList<CompatWarning>
    ) {
        if (mod.category != ModCategory.WHEELS) return
        if (mod.wheelDiameterIn == null) return
        val hasTire = allMods.any {
            it.id != mod.id && (it.tireWidthMm != null || it.category == ModCategory.TIRES)
        }
        if (hasTire) return

        out += CompatWarning(
            ruleId = "wheel_no_tire",
            severity = WarningSeverity.INFO,
            title = "No tire entry to match these wheels",
            detail = "New rims almost always mean new tires. Add a tire mod with its " +
                "width and profile so the rolling-diameter and speedometer check can run, " +
                "and so the budget reflects the real cost of the wheel swap."
        )
    }

    // ---- Rule 9: brake upgrade behind small wheels -----------------------

    private fun brakeClearanceRule(
        mod: Mod,
        car: Car?,
        allMods: List<Mod>,
        out: MutableList<CompatWarning>
    ) {
        if (mod.category != ModCategory.BRAKES) return

        val smallestRim = (allMods.mapNotNull { it.wheelDiameterIn } +
            listOfNotNull(car?.stockWheelDiameterIn)).minOrNull() ?: return
        if (smallestRim > 16) return

        out += CompatWarning(
            ruleId = "brake_clearance",
            severity = WarningSeverity.INFO,
            title = "Confirm caliper clearance inside $smallestRim\" wheels",
            detail = "Larger rotors and thicker calipers do not always fit behind small " +
                "rims. Check the caliper-to-spoke clearance for the smallest wheel you " +
                "plan to run ($smallestRim\"), including any winter or spare set."
        )
    }

    /** Highest severity across a set of warnings, or null when there are none. */
    fun peakSeverity(warnings: List<CompatWarning>): WarningSeverity? =
        warnings.maxByOrNull { it.severity.ordinal }?.severity
}
