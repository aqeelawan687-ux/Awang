package com.example

import com.example.data.dao.RiderDao
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Minimal in-memory fake of [RiderDao] used to exercise [com.example.data.repository.RiderRepository]'s
 * add/update/delete transaction logic (Bakaya/Dashboard balance math) in plain JUnit tests,
 * without needing a real Room database, Robolectric, or the Android runtime.
 */
class FakeRiderDao : RiderDao {

    private var nextRideId = 1L
    private var nextParcelId = 1L
    private var nextDebtorId = 1L
    private var nextPaymentId = 1L
    private var nextHistoryId = 1L

    val rides = linkedMapOf<Long, RideEntity>()
    val parcels = linkedMapOf<Long, ParcelEntity>()
    val debtors = linkedMapOf<Long, DebtorEntity>()
    val payments = linkedMapOf<Long, PaymentHistoryEntity>()
    val history = linkedMapOf<Long, CustomerHistoryEntity>()

    // --- RIDES ---
    override fun getAllRides(): Flow<List<RideEntity>> = MutableStateFlow(rides.values.toList())
    override suspend fun getRideById(id: Long): RideEntity? = rides[id]
    override suspend fun insertRide(ride: RideEntity): Long {
        val id = if (ride.id != 0L) ride.id else nextRideId++
        rides[id] = ride.copy(id = id)
        if (id >= nextRideId) nextRideId = id + 1
        return id
    }
    override suspend fun updateRide(ride: RideEntity) { rides[ride.id] = ride }
    override suspend fun deleteRide(ride: RideEntity) { rides.remove(ride.id) }
    override suspend fun deleteRideById(id: Long) { rides.remove(id) }
    override suspend fun clearAllRides() { rides.clear() }

    // --- PARCELS ---
    override fun getAllParcels(): Flow<List<ParcelEntity>> = MutableStateFlow(parcels.values.toList())
    override suspend fun getParcelById(id: Long): ParcelEntity? = parcels[id]
    override suspend fun insertParcel(parcel: ParcelEntity): Long {
        val id = if (parcel.id != 0L) parcel.id else nextParcelId++
        parcels[id] = parcel.copy(id = id)
        if (id >= nextParcelId) nextParcelId = id + 1
        return id
    }
    override suspend fun updateParcel(parcel: ParcelEntity) { parcels[parcel.id] = parcel }
    override suspend fun deleteParcel(parcel: ParcelEntity) { parcels.remove(parcel.id) }
    override suspend fun deleteParcelById(id: Long) { parcels.remove(id) }
    override suspend fun clearAllParcels() { parcels.clear() }

    // --- DEBTORS ---
    override fun getAllDebtors(): Flow<List<DebtorEntity>> = MutableStateFlow(debtors.values.toList())
    override suspend fun getDebtorById(id: Long): DebtorEntity? = debtors[id]
    override suspend fun findDebtorByNameOrPhone(name: String, phone: String): DebtorEntity? =
        debtors.values.find { it.name.equals(name, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone) }
    override suspend fun insertDebtor(debtor: DebtorEntity): Long {
        val id = if (debtor.id != 0L) debtor.id else nextDebtorId++
        debtors[id] = debtor.copy(id = id)
        if (id >= nextDebtorId) nextDebtorId = id + 1
        return id
    }
    override suspend fun updateDebtor(debtor: DebtorEntity) { debtors[debtor.id] = debtor }
    override suspend fun deleteDebtor(debtor: DebtorEntity) { debtors.remove(debtor.id) }
    override suspend fun deleteDebtorById(id: Long) { debtors.remove(id) }
    override suspend fun clearAllDebtors() { debtors.clear() }

    // --- PAYMENT HISTORY ---
    override fun getAllPayments(): Flow<List<PaymentHistoryEntity>> = MutableStateFlow(payments.values.toList())
    override fun getPaymentsForDebtor(debtorId: Long): Flow<List<PaymentHistoryEntity>> =
        MutableStateFlow(payments.values.filter { it.debtorId == debtorId })
    override suspend fun getPaymentById(id: Long): PaymentHistoryEntity? = payments[id]
    override suspend fun insertPayment(payment: PaymentHistoryEntity): Long {
        val id = if (payment.id != 0L) payment.id else nextPaymentId++
        payments[id] = payment.copy(id = id)
        if (id >= nextPaymentId) nextPaymentId = id + 1
        return id
    }
    override suspend fun updatePayment(payment: PaymentHistoryEntity) { payments[payment.id] = payment }
    override suspend fun deletePayment(payment: PaymentHistoryEntity) { payments.remove(payment.id) }
    override suspend fun deletePaymentById(id: Long) { payments.remove(id) }
    override suspend fun deletePaymentsByDebtorId(debtorId: Long) {
        payments.values.filter { it.debtorId == debtorId }.map { it.id }.forEach { payments.remove(it) }
    }
    override suspend fun clearAllPayments() { payments.clear() }

    // --- CUSTOMER HISTORY ---
    override fun getAllCustomerHistory(): Flow<List<CustomerHistoryEntity>> = MutableStateFlow(history.values.toList())
    override fun getHistoryForCustomer(name: String, phone: String): Flow<List<CustomerHistoryEntity>> =
        MutableStateFlow(history.values.filter { it.customerName.equals(name, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone) })
    override fun getHistoryForCustomerId(customerId: Long): Flow<List<CustomerHistoryEntity>> =
        MutableStateFlow(history.values.filter { it.customerId == customerId })
    override fun getRidesForCustomerId(customerId: Long): Flow<List<RideEntity>> =
        MutableStateFlow(rides.values.filter { it.customerId == customerId })
    override fun getParcelsForCustomerId(customerId: Long): Flow<List<ParcelEntity>> =
        MutableStateFlow(parcels.values.filter { it.customerId == customerId })
    override suspend fun deleteCustomerHistoryByCustomerId(customerId: Long) {
        history.values.filter { it.customerId == customerId }.map { it.id }.forEach { history.remove(it) }
    }
    override suspend fun insertCustomerHistory(history: CustomerHistoryEntity): Long {
        val id = if (history.id != 0L) history.id else nextHistoryId++
        this.history[id] = history.copy(id = id)
        if (id >= nextHistoryId) nextHistoryId = id + 1
        return id
    }
    override suspend fun updateCustomerHistory(history: CustomerHistoryEntity) { this.history[history.id] = history }
    override suspend fun getCustomerHistoryById(id: Long): CustomerHistoryEntity? = history[id]
    override suspend fun getCustomerHistoryByReference(referenceId: Long, activityType: String): CustomerHistoryEntity? =
        history.values.find { it.referenceId == referenceId && it.activityType == activityType }
    override suspend fun deleteCustomerHistory(history: CustomerHistoryEntity) { this.history.remove(history.id) }
    override suspend fun deleteCustomerHistoryById(id: Long) { history.remove(id) }
    override suspend fun deleteCustomerHistoryBatch(ids: List<Long>) { ids.forEach { history.remove(it) } }
    override suspend fun deleteCustomerHistoryByReference(referenceId: Long, activityType: String) {
        history.values.filter { it.referenceId == referenceId && it.activityType == activityType }.map { it.id }.forEach { history.remove(it) }
    }
    override suspend fun deleteHistoryForCustomer(name: String, phone: String) {
        history.values.filter { it.customerName.equals(name, ignoreCase = true) || (phone.isNotEmpty() && it.phone == phone) }
            .map { it.id }.forEach { history.remove(it) }
    }
    override suspend fun clearAllCustomerHistory() { history.clear() }
}
