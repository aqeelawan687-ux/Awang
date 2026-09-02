package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun riderDao(): RiderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rider_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
