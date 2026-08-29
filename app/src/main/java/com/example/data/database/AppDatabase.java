package com.example.data.database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.data.dao.RiderDao;
import com.example.data.entity.CustomerHistoryEntity;
import com.example.data.entity.DebtorEntity;
import com.example.data.entity.ParcelEntity;
import com.example.data.entity.PaymentHistoryEntity;
import com.example.data.entity.RideEntity;

@Database(
    entities = {
        DebtorEntity.class,
        PaymentHistoryEntity.class,
        ParcelEntity.class,
        RideEntity.class,
        CustomerHistoryEntity.class
    },
    version = 6,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract RiderDao riderDao();

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE rides ADD COLUMN debtorId INTEGER DEFAULT NULL");
            database.execSQL("ALTER TABLE parcels ADD COLUMN debtorId INTEGER DEFAULT NULL");
            database.execSQL("ALTER TABLE payment_history ADD COLUMN paymentType TEXT NOT NULL DEFAULT 'Cash'");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS customer_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "customerName TEXT NOT NULL, " +
                "phoneNumber TEXT NOT NULL, " +
                "actionType TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "details TEXT NOT NULL, " +
                "amount REAL NOT NULL, " +
                "timestamp INTEGER NOT NULL)");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE parcels ADD COLUMN samanName TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE parcels ADD COLUMN imageUri TEXT DEFAULT NULL");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE customer_history ADD COLUMN imageUri TEXT DEFAULT NULL");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE rides ADD COLUMN imageUri TEXT DEFAULT NULL");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        "aqeel_rider_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
