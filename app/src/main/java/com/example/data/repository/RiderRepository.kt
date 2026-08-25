package com.example.data.repository

import com.example.data.dao.RiderDao
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import kotlinx.coroutines.flow.Flow

class RiderRepository(private val dao: RiderDao) {

    val allDebtors: Flow<List<DebtorEntity>> = dao.getAllDebtors()
    val allParcels: Flow<List<ParcelEntity>> = dao.getAllParcels()
    val allRides: Flow<List<RideEntity>> = dao.getAllRides()
    val allPaymentHistory: Flow<List<PaymentHistoryEntity>> = dao.getAllPaymentHistory()

    fun getPaymentHistoryForDebtor(debtorId: Long): Flow<List<PaymentHistoryEntity>> {
        return dao.getPaymentHistoryForDebtor(debtorId)
    }

    suspend fun addDebtor(debtor: DebtorEntity): Long {
        return dao.insertDebtor(debtor)
    }

    suspend fun updateDebtor(debtor: DebtorEntity) {
        dao.updateDebtor(debtor)
    }

    suspend fun deleteDebtor(debtor: DebtorEntity) {
        dao.deleteDebtor(debtor)
    }

    suspend fun recordDebtPayment(debtor: DebtorEntity, amountPaid: Double, note: String, paymentType: String = "Cash") {
        val newDebt = (debtor.totalDebt - amountPaid).coerceAtLeast(0.0)
        val updatedDebtor = debtor.copy(
            totalDebt = newDebt,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        dao.updateDebtor(updatedDebtor)

        val history = PaymentHistoryEntity(
            debtorId = debtor.id,
            debtorName = debtor.name,
            amountPaid = amountPaid,
            timestamp = System.currentTimeMillis(),
            note = note.ifBlank { "Received Rs. ${amountPaid.toInt()}" },
            paymentType = paymentType
        )
        dao.insertPaymentHistory(history)
    }

    suspend fun insertPayment(payment: PaymentHistoryEntity) {
        dao.insertPaymentHistory(payment)
    }

    suspend fun updatePayment(payment: PaymentHistoryEntity) {
        dao.updatePaymentHistory(payment)
    }

    suspend fun deletePayment(payment: PaymentHistoryEntity) {
        dao.deletePaymentHistory(payment)
    }

    suspend fun addParcel(parcel: ParcelEntity): Long {
        return dao.insertParcel(parcel)
    }

    suspend fun updateParcel(parcel: ParcelEntity) {
        dao.updateParcel(parcel)
    }

    suspend fun deleteParcel(parcel: ParcelEntity) {
        dao.deleteParcel(parcel)
    }

    suspend fun addRide(ride: RideEntity): Long {
        return dao.insertRide(ride)
    }

    suspend fun updateRide(ride: RideEntity) {
        dao.updateRide(ride)
    }

    suspend fun deleteRide(ride: RideEntity) {
        dao.deleteRide(ride)
    }

    // Customer History
    val allCustomerHistory: Flow<List<com.example.data.entity.CustomerHistoryEntity>> = dao.getAllCustomerHistory()

    suspend fun addCustomerHistory(
        customerName: String,
        phoneNumber: String = "",
        actionType: String,
        title: String,
        details: String,
        amount: Double = 0.0,
        imageUri: String? = null
    ): Long {
        val entry = com.example.data.entity.CustomerHistoryEntity(
            customerName = customerName.trim(),
            phoneNumber = phoneNumber.trim(),
            actionType = actionType.trim(),
            title = title.trim(),
            details = details.trim(),
            amount = amount,
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri
        )
        return dao.insertCustomerHistory(entry)
    }

    suspend fun deleteCustomerHistory(history: com.example.data.entity.CustomerHistoryEntity) {
        dao.deleteCustomerHistory(history)
    }

    suspend fun deleteCustomerHistoryById(id: Long) {
        dao.deleteCustomerHistoryById(id)
    }

    suspend fun deleteCustomerHistoryByIds(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            dao.deleteCustomerHistoryByIds(ids)
        }
    }

    suspend fun deleteAllCustomerHistory() {
        dao.deleteAllCustomerHistory()
    }
}
