package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RiderDao {

    // Debtors
    @Query("SELECT * FROM debtors ORDER BY createdTimestamp DESC")
    fun getAllDebtors(): Flow<List<DebtorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtor(debtor: DebtorEntity): Long

    @Update
    suspend fun updateDebtor(debtor: DebtorEntity)

    @Delete
    suspend fun deleteDebtor(debtor: DebtorEntity)

    @Query("SELECT * FROM debtors WHERE id = :id LIMIT 1")
    suspend fun getDebtorById(id: Long): DebtorEntity?

    // Payment History
    @Query("SELECT * FROM payment_history WHERE debtorId = :debtorId ORDER BY timestamp DESC")
    fun getPaymentHistoryForDebtor(debtorId: Long): Flow<List<PaymentHistoryEntity>>

    @Query("SELECT * FROM payment_history ORDER BY timestamp DESC")
    fun getAllPaymentHistory(): Flow<List<PaymentHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentHistory(payment: PaymentHistoryEntity)

    @Update
    suspend fun updatePaymentHistory(payment: PaymentHistoryEntity)

    @Delete
    suspend fun deletePaymentHistory(payment: PaymentHistoryEntity)

    // Parcels
    @Query("SELECT * FROM parcels ORDER BY createdTimestamp DESC")
    fun getAllParcels(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE debtorId = :debtorId ORDER BY createdTimestamp DESC")
    fun getParcelsForDebtor(debtorId: Long): Flow<List<ParcelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcel(parcel: ParcelEntity): Long

    @Update
    suspend fun updateParcel(parcel: ParcelEntity)

    @Delete
    suspend fun deleteParcel(parcel: ParcelEntity)

    // Rides
    @Query("SELECT * FROM rides ORDER BY dateMillis DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE debtorId = :debtorId ORDER BY dateMillis DESC")
    fun getRidesForDebtor(debtorId: Long): Flow<List<RideEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    // Customer History
    @Query("SELECT * FROM customer_history ORDER BY timestamp DESC")
    fun getAllCustomerHistory(): Flow<List<com.example.data.entity.CustomerHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerHistory(history: com.example.data.entity.CustomerHistoryEntity): Long

    @Delete
    suspend fun deleteCustomerHistory(history: com.example.data.entity.CustomerHistoryEntity)

    @Query("DELETE FROM customer_history WHERE id = :id")
    suspend fun deleteCustomerHistoryById(id: Long)

    @Query("DELETE FROM customer_history WHERE id IN (:ids)")
    suspend fun deleteCustomerHistoryByIds(ids: List<Long>)

    @Query("DELETE FROM customer_history")
    suspend fun deleteAllCustomerHistory()
}
