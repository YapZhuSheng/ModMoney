package com.modmoney

import android.app.Application
import com.modmoney.data.GarageRepository
import com.modmoney.data.ModMoneyDatabase

/**
 * Manual dependency wiring. The app is small enough that a DI framework would
 * add more ceremony than it removes.
 */
class ModMoneyApp : Application() {

    val database: ModMoneyDatabase by lazy { ModMoneyDatabase.get(this) }

    val repository: GarageRepository by lazy {
        GarageRepository(database.carDao(), database.modDao())
    }
}
