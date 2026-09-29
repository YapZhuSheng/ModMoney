package com.modmoney

import com.modmoney.data.Car
import com.modmoney.data.Mod
import com.modmoney.data.ModCategory
import com.modmoney.data.ModPriority
import com.modmoney.data.ModStatus
import com.modmoney.data.WarningSeverity
import com.modmoney.rules.CompatibilityRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the compatibility rule engine and money semantics.
 * These run on the JVM with no device required.
 */
class CompatibilityRulesTest {

    private val city = Car(
        id = 1,
        name = "Daily City",
        model = "Honda City VX CVT",
        year = 2020,
        budgetCap = 180_000.0,
        stockWheelDiameterIn = 16,
        stockTireWidthMm = 185,
        stockTireAspect = 55
    )

    private fun mod(
        name: String = "Part",
        category: ModCategory = ModCategory.OTHER,
        est: Double = 1000.0,
        actual: Double? = null,
        status: ModStatus = ModStatus.PLANNED,
        rim: Int? = null,
        width: Double? = null,
        et: Int? = null,
        tw: Int? = null,
        ta: Int? = null,
        drop: Int? = null,
        id: Long = 100
    ) = Mod(
        id = id, carId = 1, name = name, category = category,
        estimatedPrice = est, actualPrice = actual, status = status,
        priority = ModPriority.MEDIUM,
        wheelDiameterIn = rim, wheelWidthIn = width, wheelOffsetMm = et,
        tireWidthMm = tw, tireAspect = ta, rideHeightDeltaMm = drop
    )

    // ---- the headline requirement: bigger wheel warns about profile + speedo ----

    @Test
    fun `larger than stock wheel produces diameter warning`() {
        val wheels = mod(category = ModCategory.WHEELS, rim = 17, width = 7.5, et = 42)
        val warnings = CompatibilityRules.evaluate(wheels, city, listOf(wheels))

        val diaWarning = warnings.firstOrNull { it.ruleId == "wheel_diameter_up" }
        assertNotNull("expected a wheel diameter warning", diaWarning)
        assertEquals(WarningSeverity.CAUTION, diaWarning!!.severity)
        // Must mention both the tire profile consequence and the speedometer.
        assertTrue(diaWarning.detail.contains("speedometer", ignoreCase = true))
        assertTrue(
            diaWarning.detail.contains("profile", ignoreCase = true) ||
                diaWarning.detail.contains("series", ignoreCase = true)
        )
        // Should suggest an appropriate replacement profile for a 17" rim.
        assertTrue("expected a suggested tire size", diaWarning.detail.contains("R17"))
    }

    @Test
    fun `two inch jump is critical`() {
        val wheels = mod(category = ModCategory.WHEELS, rim = 18)
        val w = CompatibilityRules.evaluate(wheels, city, listOf(wheels))
            .first { it.ruleId == "wheel_diameter_up" }
        assertEquals(WarningSeverity.CRITICAL, w.severity)
    }

    @Test
    fun `stock size wheel produces no diameter warning`() {
        val wheels = mod(category = ModCategory.WHEELS, rim = 16)
        val w = CompatibilityRules.evaluate(wheels, city, listOf(wheels))
        assertNull(w.firstOrNull { it.ruleId == "wheel_diameter_up" })
    }

    // ---- rolling diameter / speedometer offset ----

    @Test
    fun `correctly sized upsize reports good match`() {
        // 205/45 R17 vs stock 185/55 R16 is about +1% — should be INFO.
        val tires = mod(category = ModCategory.TIRES, tw = 205, ta = 45, rim = 17)
        val w = CompatibilityRules.evaluate(tires, city, listOf(tires))
            .first { it.ruleId == "rolling_diameter" }
        assertEquals(WarningSeverity.INFO, w.severity)
        assertTrue(w.title.contains("good match"))
    }

    @Test
    fun `keeping tall profile on big rim flags speedometer error`() {
        // 205/55 R17 is way taller than stock -> real speedo error.
        val tires = mod(category = ModCategory.TIRES, tw = 205, ta = 55, rim = 17)
        val w = CompatibilityRules.evaluate(tires, city, listOf(tires))
            .first { it.ruleId == "rolling_diameter" }
        assertTrue(
            "expected caution or critical, got ${w.severity}",
            w.severity != WarningSeverity.INFO
        )
        assertTrue(w.detail.contains("100 km/h"))
        // Taller tire => speedo reads low.
        assertTrue(w.detail.contains("read low"))
    }

    @Test
    fun `rolling diameter not duplicated on the wheel entry`() {
        val wheels = mod(id = 1, category = ModCategory.WHEELS, rim = 17)
        val tires = mod(id = 2, category = ModCategory.TIRES, tw = 205, ta = 45)
        val all = listOf(wheels, tires)
        // The wheel row carries no tire data, so it must not emit the tire rule.
        assertNull(
            CompatibilityRules.evaluate(wheels, city, all)
                .firstOrNull { it.ruleId == "rolling_diameter" }
        )
        assertNotNull(
            CompatibilityRules.evaluate(tires, city, all)
                .firstOrNull { it.ruleId == "rolling_diameter" }
        )
    }

    // ---- other rules ----

    @Test
    fun `low profile tire is flagged`() {
        val tires = mod(category = ModCategory.TIRES, tw = 205, ta = 35, rim = 17)
        val w = CompatibilityRules.evaluate(tires, city, listOf(tires))
        assertNotNull(w.firstOrNull { it.ruleId == "low_profile" })
    }

    @Test
    fun `big offset change warns`() {
        val wheels = mod(category = ModCategory.WHEELS, rim = 17, et = 25)
        val w = CompatibilityRules.evaluate(wheels, city, listOf(wheels))
            .firstOrNull { it.ruleId == "offset_change" }
        assertNotNull(w)
        assertTrue(w!!.detail.contains("outward"))
    }

    @Test
    fun `lowering plus big wheels cross references both mods`() {
        val wheels = mod(id = 1, name = "17s", category = ModCategory.WHEELS, rim = 17)
        val springs = mod(id = 2, category = ModCategory.SUSPENSION, drop = -35)
        val w = CompatibilityRules.evaluate(springs, city, listOf(wheels, springs))
            .firstOrNull { it.ruleId == "lowered_plus_big_wheel" }
        assertNotNull("expected the combined lowering + wheel warning", w)
        assertTrue(w!!.detail.contains("17s"))
    }

    @Test
    fun `wheels with no tire entry are flagged`() {
        val wheels = mod(category = ModCategory.WHEELS, rim = 17)
        val w = CompatibilityRules.evaluate(wheels, city, listOf(wheels))
        assertNotNull(w.firstOrNull { it.ruleId == "wheel_no_tire" })
    }

    @Test
    fun `mod with no specs produces no warnings`() {
        val plain = mod(name = "Floor mats", category = ModCategory.INTERIOR)
        assertTrue(CompatibilityRules.evaluate(plain, city, listOf(plain)).isEmpty())
    }

    @Test
    fun `car without stock reference disables dependent rules`() {
        val bare = city.copy(
            stockWheelDiameterIn = null,
            stockTireWidthMm = null,
            stockTireAspect = null
        )
        val wheels = mod(category = ModCategory.WHEELS, rim = 19, tw = 225, ta = 35)
        val w = CompatibilityRules.evaluate(wheels, bare, listOf(wheels))
        assertNull(w.firstOrNull { it.ruleId == "wheel_diameter_up" })
        assertNull(w.firstOrNull { it.ruleId == "rolling_diameter" })
    }

    @Test
    fun `warnings are sorted most severe first`() {
        val wheels = mod(category = ModCategory.WHEELS, rim = 18, et = 20, tw = 225, ta = 30)
        val w = CompatibilityRules.evaluate(wheels, city, listOf(wheels))
        assertTrue(w.size > 1)
        val severities = w.map { it.severity.ordinal }
        assertEquals(severities.sortedDescending(), severities)
    }

    // ---- money semantics ----

    @Test
    fun `planned mods do not count as spent`() {
        assertEquals(0.0, mod(est = 5000.0, status = ModStatus.PLANNED).spent, 0.001)
    }

    @Test
    fun `bought mod without actual price falls back to estimate`() {
        assertEquals(5000.0, mod(est = 5000.0, status = ModStatus.BOUGHT).spent, 0.001)
    }

    @Test
    fun `actual price wins over estimate when spent`() {
        val m = mod(est = 5000.0, actual = 4200.0, status = ModStatus.INSTALLED)
        assertEquals(4200.0, m.spent, 0.001)
        assertEquals(-800.0, m.variance!!, 0.001)
    }

    @Test
    fun `projected uses actual or estimate regardless of status`() {
        assertEquals(5000.0, mod(est = 5000.0, status = ModStatus.PLANNED).projected, 0.001)
        assertEquals(
            4200.0,
            mod(est = 5000.0, actual = 4200.0, status = ModStatus.PLANNED).projected,
            0.001
        )
    }
}
