package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.RiderDao
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity

@Database(
    entities = [
        RideEntity::class,
        ParcelEntity::class,
        DebtorEntity::class,
        PaymentHistoryEntity::class,
        CustomerHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun riderDao(): RiderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debtors ADD COLUMN photoUri TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE debtors ADD COLUMN address TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE debtors ADD COLUMN location TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE rides ADD COLUMN customerId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE parcels ADD COLUMN customerId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE parcels ADD COLUMN shopName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE parcels ADD COLUMN samanCharges REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE customer_history ADD COLUMN customerId INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rider_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
