package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RiderDao {

    // --- RIDES ---
    @Query("SELECT * FROM rides ORDER BY rideDate DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :id")
    suspend fun getRideById(id: Long): RideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long

    @Update
    suspend fun updateRide(ride: RideEntity)

    @Delete
    suspend fun deleteRide(ride: RideEntity)

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteRideById(id: Long)

    @Query("DELETE FROM rides")
    suspend fun clearAllRides()

    // --- PARCELS ---
    @Query("SELECT * FROM parcels ORDER BY date DESC")
    fun getAllParcels(): Flow<List<ParcelEntity>>

    @Query("SELECT * FROM parcels WHERE id = :id")
    suspend fun getParcelById(id: Long): ParcelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParcel(parcel: ParcelEntity): Long

    @Update
    suspend fun updateParcel(parcel: ParcelEntity)

    @Delete
    suspend fun deleteParcel(parcel: ParcelEntity)

    @Query("DELETE FROM parcels WHERE id = :id")
    suspend fun deleteParcelById(id: Long)

    @Query("DELETE FROM parcels")
    suspend fun clearAllParcels()

    // --- DEBTORS ---
    @Query("SELECT * FROM debtors ORDER BY lastUpdated DESC")
    fun getAllDebtors(): Flow<List<DebtorEntity>>

    @Query("SELECT * FROM debtors WHERE id = :id")
    suspend fun getDebtorById(id: Long): DebtorEntity?

    @Query("SELECT * FROM debtors WHERE (LOWER(name) = LOWER(:name) OR (phone = :phone AND phone != '')) LIMIT 1")
    suspend fun findDebtorByNameOrPhone(name: String, phone: String): DebtorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtor(debtor: DebtorEntity): Long

    @Update
    suspend fun updateDebtor(debtor: DebtorEntity)

    @Delete
    suspend fun deleteDebtor(debtor: DebtorEntity)

    @Query("DELETE FROM debtors WHERE id = :id")
    suspend fun deleteDebtorById(id: Long)

    @Query("DELETE FROM debtors")
    suspend fun clearAllDebtors()

    // --- PAYMENT HISTORY ---
    @Query("SELECT * FROM payment_history ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentHistoryEntity>>

    @Query("SELECT * FROM payment_history WHERE debtorId = :debtorId ORDER BY paymentDate DESC")
    fun getPaymentsForDebtor(debtorId: Long): Flow<List<PaymentHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentHistoryEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentHistoryEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentHistoryEntity)

    @Query("DELETE FROM payment_history WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("DELETE FROM payment_history WHERE debtorId = :debtorId")
    suspend fun deletePaymentsByDebtorId(debtorId: Long)

    @Query("DELETE FROM payment_history")
    suspend fun clearAllPayments()

    // --- CUSTOMER HISTORY ---
    @Query("SELECT * FROM customer_history ORDER BY timestamp DESC")
    fun getAllCustomerHistory(): Flow<List<CustomerHistoryEntity>>

    @Query("SELECT * FROM customer_history WHERE (LOWER(customerName) = LOWER(:name) OR (phone = :phone AND phone != '')) ORDER BY timestamp DESC")
    fun getHistoryForCustomer(name: String, phone: String): Flow<List<CustomerHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerHistory(history: CustomerHistoryEntity): Long

    @Delete
    suspend fun deleteCustomerHistory(history: CustomerHistoryEntity)

    @Query("DELETE FROM customer_history WHERE id = :id")
    suspend fun deleteCustomerHistoryById(id: Long)

    @Query("DELETE FROM customer_history WHERE id IN (:ids)")
    suspend fun deleteCustomerHistoryBatch(ids: List<Long>)

    @Query("DELETE FROM customer_history")
    suspend fun clearAllCustomerHistory()
}
