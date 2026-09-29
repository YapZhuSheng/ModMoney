package com.modmoney.data

/**
 * First-run sample content: one fully worked Honda City build.
 *
 * The numbers are deliberately chosen to exercise every part of the UI:
 *  - a mix of planned / bought / installed rows across all three priorities,
 *  - actual prices both under and over the estimate (variance display),
 *  - spend comfortably under the cap but *projected* spend over it, so the
 *    dashboard's over-budget projection warning is visible out of the box,
 *  - a 17" wheel on a car with 16" stock wheels, which trips the
 *    wheel-diameter compatibility rule, paired with a correctly sized tire
 *    so the rolling-diameter check reports a good match.
 */
object SampleData {

    suspend fun seed(carDao: CarDao, modDao: ModDao) {
        if (carDao.count() > 0) return

        val carId = carDao.insert(
            Car(
                name = "Daily City",
                model = "Honda City VX CVT",
                year = 2020,
                budgetCap = 180_000.0,
                stockWheelDiameterIn = 16,
                stockTireWidthMm = 185,
                stockTireAspect = 55,
                notes = "5th gen (GN2). Stock 16\" alloys with 185/55 R16. " +
                    "Keeping it street-friendly — no rubbing, no warning lights."
            )
        )

        val mods = listOf(
            Mod(
                carId = carId,
                name = "17x7.5 ET42 flow-formed alloys",
                category = ModCategory.WHEELS,
                estimatedPrice = 48_000.0,
                actualPrice = 46_500.0,
                status = ModStatus.BOUGHT,
                priority = ModPriority.HIGH,
                compatibilityNotes = "5x114.3, 64.1 hub. Went one inch up from stock. " +
                    "Need to confirm no strut clearance issue at full lock.",
                wheelDiameterIn = 17,
                wheelWidthIn = 7.5,
                wheelOffsetMm = 42
            ),
            Mod(
                carId = carId,
                name = "205/45 R17 Michelin Pilot Sport 4",
                category = ModCategory.TIRES,
                estimatedPrice = 32_000.0,
                actualPrice = 33_200.0,
                status = ModStatus.INSTALLED,
                priority = ModPriority.HIGH,
                compatibilityNotes = "Sized to keep rolling diameter near stock so the " +
                    "speedo stays honest.",
                tireWidthMm = 205,
                tireAspect = 45
            ),
            Mod(
                carId = carId,
                name = "Lowering springs (-35 mm)",
                category = ModCategory.SUSPENSION,
                estimatedPrice = 18_000.0,
                status = ModStatus.PLANNED,
                priority = ModPriority.MEDIUM,
                compatibilityNotes = "Want the gap gone, but not scraping every speed bump.",
                rideHeightDeltaMm = -35
            ),
            Mod(
                carId = carId,
                name = "Front strut brace",
                category = ModCategory.SUSPENSION,
                estimatedPrice = 6_500.0,
                actualPrice = 6_200.0,
                status = ModStatus.INSTALLED,
                priority = ModPriority.LOW,
                compatibilityNotes = "Clears the engine cover fine."
            ),
            Mod(
                carId = carId,
                name = "Cat-back exhaust, 2.25\"",
                category = ModCategory.EXHAUST,
                estimatedPrice = 24_000.0,
                status = ModStatus.PLANNED,
                priority = ModPriority.MEDIUM,
                compatibilityNotes = "Must stay quiet enough to pass the annual check."
            ),
            Mod(
                carId = carId,
                name = "Cold air intake",
                category = ModCategory.ENGINE,
                estimatedPrice = 9_500.0,
                actualPrice = 8_900.0,
                status = ModStatus.BOUGHT,
                priority = ModPriority.MEDIUM,
                compatibilityNotes = "Heat shield included; watch for a lean CEL after fitting."
            ),
            Mod(
                carId = carId,
                name = "Drilled front rotors + performance pads",
                category = ModCategory.BRAKES,
                estimatedPrice = 21_000.0,
                status = ModStatus.PLANNED,
                priority = ModPriority.HIGH,
                compatibilityNotes = "262 mm front. Confirm pad compound is OK cold — daily driver."
            ),
            Mod(
                carId = carId,
                name = "LED headlight conversion",
                category = ModCategory.LIGHTING,
                estimatedPrice = 7_800.0,
                actualPrice = 7_800.0,
                status = ModStatus.INSTALLED,
                priority = ModPriority.LOW,
                compatibilityNotes = "Projector housing, so no scatter. CANbus decoder fitted."
            ),
            Mod(
                carId = carId,
                name = "9\" Android head unit",
                category = ModCategory.AUDIO,
                estimatedPrice = 16_500.0,
                actualPrice = 15_900.0,
                status = ModStatus.INSTALLED,
                priority = ModPriority.MEDIUM,
                compatibilityNotes = "Retained steering-wheel controls via canbox."
            ),
            Mod(
                carId = carId,
                name = "Front lip + side skirts",
                category = ModCategory.EXTERIOR,
                estimatedPrice = 14_000.0,
                status = ModStatus.PLANNED,
                priority = ModPriority.LOW,
                compatibilityNotes = "PU, not ABS — needs to survive driveway ramps."
            ),
            Mod(
                carId = carId,
                name = "Door + boot sound deadening",
                category = ModCategory.INTERIOR,
                estimatedPrice = 8_000.0,
                status = ModStatus.PLANNED,
                priority = ModPriority.LOW,
                compatibilityNotes = "Butyl sheets, 4 doors."
            )
        )

        mods.forEach { modDao.insert(it) }
    }
}
