package com.example.data.repository

import com.example.data.dao.RiderDao
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import kotlinx.coroutines.flow.Flow

class RiderRepository(private val dao: RiderDao) {

    val allRides: Flow<List<RideEntity>> = dao.getAllRides()
    val allParcels: Flow<List<ParcelEntity>> = dao.getAllParcels()
    val allDebtors: Flow<List<DebtorEntity>> = dao.getAllDebtors()
    val allPayments: Flow<List<PaymentHistoryEntity>> = dao.getAllPayments()
    val allCustomerHistory: Flow<List<CustomerHistoryEntity>> = dao.getAllCustomerHistory()

    fun getPaymentsForDebtor(debtorId: Long): Flow<List<PaymentHistoryEntity>> =
        dao.getPaymentsForDebtor(debtorId)

    fun getCustomerHistory(name: String, phone: String): Flow<List<CustomerHistoryEntity>> =
        dao.getHistoryForCustomer(name, phone)

    suspend fun addRide(ride: RideEntity): Long {
        val rideId = dao.insertRide(ride)
        val remaining = ride.remainingBakaya
        if (remaining > 0) {
            val existing = dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
            if (existing != null) {
                dao.updateDebtor(
                    existing.copy(
                        totalDebt = existing.totalDebt + remaining,
                        remainingDebt = existing.remainingDebt + remaining,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                dao.insertDebtor(
                    DebtorEntity(
                        name = ride.customerName,
                        phone = ride.phone,
                        totalDebt = remaining,
                        remainingDebt = remaining,
                        lastUpdated = System.currentTimeMillis(),
                        notes = "Auto-created from Ride #${rideId}"
                    )
                )
            }
        }
        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerName = ride.customerName,
                phone = ride.phone,
                activityType = "RIDE",
                details = "Ride from ${ride.pickupLocation} to ${ride.dropoffLocation} (Fare: Rs. ${ride.fare}, Paid: Rs. ${ride.amountPaid}, Bakaya: Rs. ${ride.remainingBakaya})",
                amount = ride.fare,
                bakayaAmount = ride.remainingBakaya,
                timestamp = ride.rideDate,
                referenceId = rideId
            )
        )
        return rideId
    }

    suspend fun updateRide(ride: RideEntity) {
        val oldRide = dao.getRideById(ride.id)
        if (oldRide != null) {
            val diffBakaya = ride.remainingBakaya - oldRide.remainingBakaya
            if (diffBakaya != 0.0) {
                val debtor = dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
                if (debtor != null) {
                    val newRemaining = (debtor.remainingDebt + diffBakaya).coerceAtLeast(0.0)
                    val newTotal = (debtor.totalDebt + diffBakaya).coerceAtLeast(0.0)
                    dao.updateDebtor(
                        debtor.copy(
                            totalDebt = newTotal,
                            remainingDebt = newRemaining,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else if (ride.remainingBakaya > 0) {
                    dao.insertDebtor(
                        DebtorEntity(
                            name = ride.customerName,
                            phone = ride.phone,
                            totalDebt = ride.remainingBakaya,
                            remainingDebt = ride.remainingBakaya,
                            lastUpdated = System.currentTimeMillis(),
                            notes = "Auto-created from edited Ride #${ride.id}"
                        )
                    )
                }
            }

            val existingHistory = dao.getCustomerHistoryByReference(ride.id, "RIDE")
            val historyDetails = "Ride from ${ride.pickupLocation} to ${ride.dropoffLocation} (Fare: Rs. ${ride.fare.toInt()}, Paid: Rs. ${ride.amountPaid.toInt()}, Bakaya: Rs. ${ride.remainingBakaya.toInt()})"
            if (existingHistory != null) {
                dao.updateCustomerHistory(
                    existingHistory.copy(
                        customerName = ride.customerName,
                        phone = ride.phone,
                        details = historyDetails,
                        amount = ride.fare,
                        bakayaAmount = ride.remainingBakaya,
                        timestamp = existingHistory.timestamp
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = ride.customerName,
                        phone = ride.phone,
                        activityType = "RIDE",
                        details = historyDetails,
                        amount = ride.fare,
                        bakayaAmount = ride.remainingBakaya,
                        timestamp = ride.rideDate,
                        referenceId = ride.id
                    )
                )
            }
        }
        dao.updateRide(ride)
    }

    suspend fun deleteRide(ride: RideEntity) {
        if (ride.remainingBakaya > 0) {
            val debtor = dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
            if (debtor != null) {
                val newRemaining = (debtor.remainingDebt - ride.remainingBakaya).coerceAtLeast(0.0)
                val newTotal = (debtor.totalDebt - ride.remainingBakaya).coerceAtLeast(0.0)
                dao.updateDebtor(debtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
            }
        }
        val history = dao.getCustomerHistoryByReference(ride.id, "RIDE")
        if (history != null) {
            dao.deleteCustomerHistory(history)
        }
        dao.deleteRide(ride)
    }

    suspend fun addParcel(parcel: ParcelEntity): Long {
        val parcelId = dao.insertParcel(parcel)
        val remaining = parcel.remainingBakaya
        if (remaining > 0) {
            val existing = dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
            if (existing != null) {
                dao.updateDebtor(
                    existing.copy(
                        totalDebt = existing.totalDebt + remaining,
                        remainingDebt = existing.remainingDebt + remaining,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                dao.insertDebtor(
                    DebtorEntity(
                        name = parcel.senderName,
                        phone = parcel.senderPhone,
                        totalDebt = remaining,
                        remainingDebt = remaining,
                        lastUpdated = System.currentTimeMillis(),
                        notes = "Auto-created from Parcel #${parcelId}"
                    )
                )
            }
        }
        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerName = parcel.senderName,
                phone = parcel.senderPhone,
                activityType = "PARCEL",
                details = "Parcel to ${parcel.receiverName} (${parcel.deliveryAddress}) Charges: Rs. ${parcel.deliveryCharges}, Paid: Rs. ${parcel.amountPaid}, Bakaya: Rs. ${parcel.remainingBakaya}",
                amount = parcel.deliveryCharges,
                bakayaAmount = parcel.remainingBakaya,
                timestamp = parcel.date,
                referenceId = parcelId
            )
        )
        return parcelId
    }

    suspend fun updateParcel(parcel: ParcelEntity) {
        val oldParcel = dao.getParcelById(parcel.id)
        if (oldParcel != null) {
            val diffBakaya = parcel.remainingBakaya - oldParcel.remainingBakaya
            if (diffBakaya != 0.0) {
                val debtor = dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
                if (debtor != null) {
                    val newRemaining = (debtor.remainingDebt + diffBakaya).coerceAtLeast(0.0)
                    val newTotal = (debtor.totalDebt + diffBakaya).coerceAtLeast(0.0)
                    dao.updateDebtor(
                        debtor.copy(
                            totalDebt = newTotal,
                            remainingDebt = newRemaining,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } else if (parcel.remainingBakaya > 0) {
                    dao.insertDebtor(
                        DebtorEntity(
                            name = parcel.senderName,
                            phone = parcel.senderPhone,
                            totalDebt = parcel.remainingBakaya,
                            remainingDebt = parcel.remainingBakaya,
                            lastUpdated = System.currentTimeMillis(),
                            notes = "Auto-created from edited Parcel #${parcel.id}"
                        )
                    )
                }
            }

            val existingHistory = dao.getCustomerHistoryByReference(parcel.id, "PARCEL")
            val historyDetails = "Parcel to ${parcel.receiverName} (${parcel.deliveryAddress}) Charges: Rs. ${parcel.deliveryCharges.toInt()}, Paid: Rs. ${parcel.amountPaid.toInt()}, Bakaya: Rs. ${parcel.remainingBakaya.toInt()}"
            if (existingHistory != null) {
                dao.updateCustomerHistory(
                    existingHistory.copy(
                        customerName = parcel.senderName,
                        phone = parcel.senderPhone,
                        details = historyDetails,
                        amount = parcel.deliveryCharges,
                        bakayaAmount = parcel.remainingBakaya,
                        timestamp = existingHistory.timestamp
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = parcel.senderName,
                        phone = parcel.senderPhone,
                        activityType = "PARCEL",
                        details = historyDetails,
                        amount = parcel.deliveryCharges,
                        bakayaAmount = parcel.remainingBakaya,
                        timestamp = parcel.date,
                        referenceId = parcel.id
                    )
                )
            }
        }
        dao.updateParcel(parcel)
    }

    suspend fun deleteParcel(parcel: ParcelEntity) {
        if (parcel.remainingBakaya > 0) {
            val debtor = dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
            if (debtor != null) {
                val newRemaining = (debtor.remainingDebt - parcel.remainingBakaya).coerceAtLeast(0.0)
                val newTotal = (debtor.totalDebt - parcel.remainingBakaya).coerceAtLeast(0.0)
                dao.updateDebtor(debtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
            }
        }
        val history = dao.getCustomerHistoryByReference(parcel.id, "PARCEL")
        if (history != null) {
            dao.deleteCustomerHistory(history)
        }
        dao.deleteParcel(parcel)
    }

    suspend fun addDebtor(debtor: DebtorEntity): Long = dao.insertDebtor(debtor)

    suspend fun updateDebtor(debtor: DebtorEntity) = dao.updateDebtor(debtor)

    suspend fun deleteDebtor(debtor: DebtorEntity) {
        dao.deletePaymentsByDebtorId(debtor.id)
        dao.deleteDebtor(debtor)
    }

    suspend fun addPayment(debtorId: Long, amount: Double, note: String): Long {
        val payment = PaymentHistoryEntity(
            debtorId = debtorId,
            amountPaid = amount,
            paymentDate = System.currentTimeMillis(),
            note = note
        )
        val paymentId = dao.insertPayment(payment)
        val debtor = dao.getDebtorById(debtorId)
        if (debtor != null) {
            val updatedRemaining = (debtor.remainingDebt - amount).coerceAtLeast(0.0)
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = updatedRemaining,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            dao.insertCustomerHistory(
                CustomerHistoryEntity(
                    customerName = debtor.name,
                    phone = debtor.phone,
                    activityType = "PAYMENT",
                    details = "Payment of Rs. $amount received. Remaining Debt: Rs. $updatedRemaining (${note})",
                    amount = amount,
                    bakayaAmount = updatedRemaining,
                    timestamp = System.currentTimeMillis(),
                    referenceId = paymentId
                )
            )
        }
        return paymentId
    }

    suspend fun updatePayment(payment: PaymentHistoryEntity, oldAmount: Double) {
        val diff = payment.amountPaid - oldAmount
        val debtor = dao.getDebtorById(payment.debtorId)
        if (debtor != null) {
            val newRemaining = (debtor.remainingDebt - diff).coerceAtLeast(0.0)
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = newRemaining,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
        dao.updatePayment(payment)
    }

    suspend fun deletePayment(payment: PaymentHistoryEntity) {
        val debtor = dao.getDebtorById(payment.debtorId)
        if (debtor != null) {
            val newRemaining = debtor.remainingDebt + payment.amountPaid
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = newRemaining,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
        dao.deletePayment(payment)
    }

    suspend fun updateCustomerBakaya(name: String, phone: String, newBakaya: Double) {
        val debtor = dao.findDebtorByNameOrPhone(name, phone)
        if (debtor != null) {
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = newBakaya,
                    totalDebt = maxOf(debtor.totalDebt, newBakaya),
                    lastUpdated = System.currentTimeMillis()
                )
            )
        } else if (newBakaya > 0) {
            dao.insertDebtor(
                DebtorEntity(
                    name = name,
                    phone = phone,
                    totalDebt = newBakaya,
                    remainingDebt = newBakaya,
                    lastUpdated = System.currentTimeMillis(),
                    notes = "Added from Customer Ledger"
                )
            )
        }
    }

    suspend fun deleteCustomerBakaya(name: String, phone: String) {
        val debtor = dao.findDebtorByNameOrPhone(name, phone)
        if (debtor != null) {
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = 0.0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteCustomerHistory(history: CustomerHistoryEntity) {
        dao.deleteCustomerHistory(history)
    }

    suspend fun deleteCustomerHistoryById(id: Long) {
        dao.deleteCustomerHistoryById(id)
    }

    suspend fun deleteCustomerHistoryBatch(ids: List<Long>) {
        dao.deleteCustomerHistoryBatch(ids)
    }

    suspend fun deleteAllCustomerHistory() {
        dao.clearAllCustomerHistory()
    }

    suspend fun resetAllData() {
        dao.clearAllRides()
        dao.clearAllParcels()
        dao.clearAllDebtors()
        dao.clearAllPayments()
        dao.clearAllCustomerHistory()
    }
}
