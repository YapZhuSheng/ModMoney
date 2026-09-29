package com.modmoney

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.modmoney.data.ModCategory
import com.modmoney.data.ModMoneyDatabase
import com.modmoney.data.ModStatus
import com.modmoney.data.SampleData
import com.modmoney.rules.CompatibilityRules
import com.modmoney.data.WarningSeverity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises the real Room schema, DAO aggregate SQL and the seeded sample
 * build. Runs on the JVM via Robolectric, so it validates that the generated
 * Room code and the SQL in [com.modmoney.data.ModDao] actually work — not just
 * that the Kotlin compiles.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DatabaseSeedTest {

    private lateinit var db: ModMoneyDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ModMoneyDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `sample data seeds one car with mods`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())

        val cars = db.carDao().observeAll().first()
        assertEquals(1, cars.size)
        val car = cars.first()
        assertEquals("Honda City VX CVT", car.model)
        assertEquals(2020, car.year)
        assertEquals(180_000.0, car.budgetCap, 0.001)
        assertEquals(16, car.stockWheelDiameterIn)

        val mods = db.modDao().observeForCar(car.id).first()
        assertEquals(11, mods.size)
    }

    @Test
    fun `seeding is idempotent`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        SampleData.seed(db.carDao(), db.modDao())
        assertEquals(1, db.carDao().count())
    }

    @Test
    fun `totals match the documented money rules`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        val car = db.carDao().observeAll().first().first()

        val spent = db.modDao().observeTotalSpent(car.id).first()
        val projected = db.modDao().observeTotalProjected(car.id).first()

        // Bought/installed actual prices: 46500+33200+6200+8900+7800+15900
        assertEquals(118_500.0, spent, 0.001)
        // Plus planned estimates: 18000+24000+21000+14000+8000 = 85000
        assertEquals(203_500.0, projected, 0.001)

        // Verify the SQL aggregate agrees with the Kotlin property.
        val mods = db.modDao().listForCar(car.id)
        assertEquals(mods.sumOf { it.spent }, spent, 0.001)
        assertEquals(mods.sumOf { it.projected }, projected, 0.001)
    }

    @Test
    fun `sample build is under cap but projected over cap`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        val car = db.carDao().observeAll().first().first()
        val spent = db.modDao().observeTotalSpent(car.id).first()
        val projected = db.modDao().observeTotalProjected(car.id).first()

        assertTrue("spend should be under the cap", spent < car.budgetCap)
        assertTrue("projection should exceed the cap", projected > car.budgetCap)
    }

    @Test
    fun `category breakdown groups correctly`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        val car = db.carDao().observeAll().first().first()
        val cats = db.modDao().observeCategorySpend(car.id).first()

        // Suspension has two entries (springs planned, strut brace installed).
        val susp = cats.first { it.category == ModCategory.SUSPENSION }
        assertEquals(2, susp.count)
        assertEquals(6_200.0, susp.spent, 0.001)          // only the brace is installed
        assertEquals(24_200.0, susp.projected, 0.001)     // + 18000 planned springs

        // Wheels: bought, actual price counted.
        val wheels = cats.first { it.category == ModCategory.WHEELS }
        assertEquals(46_500.0, wheels.spent, 0.001)

        // Every mod is accounted for exactly once.
        assertEquals(11, cats.sumOf { it.count })
        // Sorted with the biggest spend first.
        val spends = cats.map { it.spent }
        assertEquals(spends.sortedDescending(), spends)
    }

    @Test
    fun `enum values survive a database round trip`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        val car = db.carDao().observeAll().first().first()
        val mods = db.modDao().listForCar(car.id)

        val tires = mods.first { it.category == ModCategory.TIRES }
        assertEquals(ModStatus.INSTALLED, tires.status)
        assertEquals(205, tires.tireWidthMm)
        assertEquals(45, tires.tireAspect)

        val wheels = mods.first { it.category == ModCategory.WHEELS }
        assertEquals(17, wheels.wheelDiameterIn)
        assertEquals(7.5, wheels.wheelWidthIn!!, 0.001)
        assertEquals(42, wheels.wheelOffsetMm)

        val springs = mods.first { it.name.contains("Lowering") }
        assertEquals(-35, springs.rideHeightDeltaMm)
    }

    @Test
    fun `deleting a car cascades to its mods`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        val car = db.carDao().observeAll().first().first()
        db.carDao().deleteById(car.id)
        assertEquals(0, db.modDao().listForCar(car.id).size)
    }

    @Test
    fun `seeded build surfaces the wheel upsize warning`() = runTest {
        SampleData.seed(db.carDao(), db.modDao())
        val car = db.carDao().observeAll().first().first()
        val mods = db.modDao().listForCar(car.id)

        val wheels = mods.first { it.category == ModCategory.WHEELS }
        val warnings = CompatibilityRules.evaluate(wheels, car, mods)
        assertNotNull(
            "the 17\" wheel on a 16\" car should warn",
            warnings.firstOrNull { it.ruleId == "wheel_diameter_up" }
        )

        // And the matched tire should read as a good rolling-diameter match.
        val tires = mods.first { it.category == ModCategory.TIRES }
        val tireWarnings = CompatibilityRules.evaluate(tires, car, mods)
        val rolling = tireWarnings.first { it.ruleId == "rolling_diameter" }
        assertEquals(WarningSeverity.INFO, rolling.severity)
    }
}
