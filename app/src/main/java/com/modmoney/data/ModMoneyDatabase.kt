package com.modmoney.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Car::class, Mod::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ModMoneyDatabase : RoomDatabase() {

    abstract fun carDao(): CarDao
    abstract fun modDao(): ModDao

    companion object {
        private const val DB_NAME = "modmoney.db"

        @Volatile private var instance: ModMoneyDatabase? = null

        fun get(context: Context): ModMoneyDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): ModMoneyDatabase {
            // Seeding runs on first create only. It is done from a callback with
            // an application-scoped coroutine rather than inside the Room
            // transaction so that suspend DAO methods can be reused.
            lateinit var db: ModMoneyDatabase
            val scope = CoroutineScope(Dispatchers.IO)

            db = Room.databaseBuilder(
                context.applicationContext,
                ModMoneyDatabase::class.java,
                DB_NAME
            )
                .addCallback(object : Callback() {
                    override fun onCreate(connection: SupportSQLiteDatabase) {
                        super.onCreate(connection)
                        scope.launch {
                            SampleData.seed(db.carDao(), db.modDao())
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()

            return db
        }
    }
}
