package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.dao.RiderDao
import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import kotlinx.coroutines.flow.Flow

class RiderRepository(
    private val dao: RiderDao,
    private val database: AppDatabase? = null
) {

    val allRides: Flow<List<RideEntity>> = dao.getAllRides()
    val allParcels: Flow<List<ParcelEntity>> = dao.getAllParcels()
    val allDebtors: Flow<List<DebtorEntity>> = dao.getAllDebtors()
    val allPayments: Flow<List<PaymentHistoryEntity>> = dao.getAllPayments()
    val allCustomerHistory: Flow<List<CustomerHistoryEntity>> = dao.getAllCustomerHistory()

    fun getPaymentsForDebtor(debtorId: Long): Flow<List<PaymentHistoryEntity>> =
        dao.getPaymentsForDebtor(debtorId)

    fun getCustomerHistory(name: String, phone: String): Flow<List<CustomerHistoryEntity>> =
        dao.getHistoryForCustomer(name, phone)

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    suspend fun addRide(ride: RideEntity): Long = runInTransaction {
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
        val historyDetails = "Ride from ${ride.pickupLocation} to ${ride.dropoffLocation} (Fare: Rs. ${ride.fare.toInt()}, Paid: Rs. ${ride.amountPaid.toInt()}, Bakaya: Rs. ${ride.remainingBakaya.toInt()})"
        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerName = ride.customerName,
                phone = ride.phone,
                activityType = "RIDE",
                details = historyDetails,
                amount = ride.fare,
                bakayaAmount = ride.remainingBakaya,
                timestamp = ride.rideDate,
                referenceId = rideId
            )
        )
        rideId
    }

    suspend fun updateRide(ride: RideEntity) = runInTransaction {
        val oldRide = dao.getRideById(ride.id)
        if (oldRide != null) {
            val preservedDate = oldRide.rideDate
            val updatedRide = ride.copy(rideDate = preservedDate)

            val customerChanged = !oldRide.customerName.equals(updatedRide.customerName, ignoreCase = true) ||
                    (oldRide.phone.isNotEmpty() && updatedRide.phone.isNotEmpty() && oldRide.phone != updatedRide.phone)

            if (customerChanged) {
                if (oldRide.remainingBakaya > 0) {
                    val oldDebtor = dao.findDebtorByNameOrPhone(oldRide.customerName, oldRide.phone)
                    if (oldDebtor != null) {
                        val newRemaining = (oldDebtor.remainingDebt - oldRide.remainingBakaya).coerceAtLeast(0.0)
                        val newTotal = (oldDebtor.totalDebt - oldRide.remainingBakaya).coerceAtLeast(0.0)
                        dao.updateDebtor(oldDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    }
                }
                if (updatedRide.remainingBakaya > 0) {
                    val newDebtor = dao.findDebtorByNameOrPhone(updatedRide.customerName, updatedRide.phone)
                    if (newDebtor != null) {
                        val newRemaining = newDebtor.remainingDebt + updatedRide.remainingBakaya
                        val newTotal = newDebtor.totalDebt + updatedRide.remainingBakaya
                        dao.updateDebtor(newDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    } else {
                        dao.insertDebtor(
                            DebtorEntity(
                                name = updatedRide.customerName,
                                phone = updatedRide.phone,
                                totalDebt = updatedRide.remainingBakaya,
                                remainingDebt = updatedRide.remainingBakaya,
                                lastUpdated = System.currentTimeMillis(),
                                notes = "Auto-created from edited Ride #${updatedRide.id}"
                            )
                        )
                    }
                }
            } else {
                val diffBakaya = updatedRide.remainingBakaya - oldRide.remainingBakaya
                if (diffBakaya != 0.0) {
                    val debtor = dao.findDebtorByNameOrPhone(updatedRide.customerName, updatedRide.phone)
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
                    } else if (updatedRide.remainingBakaya > 0) {
                        dao.insertDebtor(
                            DebtorEntity(
                                name = updatedRide.customerName,
                                phone = updatedRide.phone,
                                totalDebt = updatedRide.remainingBakaya,
                                remainingDebt = updatedRide.remainingBakaya,
                                lastUpdated = System.currentTimeMillis(),
                                notes = "Auto-created from edited Ride #${updatedRide.id}"
                            )
                        )
                    }
                }
            }

            val existingHistory = dao.getCustomerHistoryByReference(updatedRide.id, "RIDE")
            val historyDetails = "Ride from ${updatedRide.pickupLocation} to ${updatedRide.dropoffLocation} (Fare: Rs. ${updatedRide.fare.toInt()}, Paid: Rs. ${updatedRide.amountPaid.toInt()}, Bakaya: Rs. ${updatedRide.remainingBakaya.toInt()})"
            if (existingHistory != null) {
                dao.updateCustomerHistory(
                    existingHistory.copy(
                        customerName = updatedRide.customerName,
                        phone = updatedRide.phone,
                        details = historyDetails,
                        amount = updatedRide.fare,
                        bakayaAmount = updatedRide.remainingBakaya,
                        timestamp = existingHistory.timestamp
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = updatedRide.customerName,
                        phone = updatedRide.phone,
                        activityType = "RIDE",
                        details = historyDetails,
                        amount = updatedRide.fare,
                        bakayaAmount = updatedRide.remainingBakaya,
                        timestamp = preservedDate,
                        referenceId = updatedRide.id
                    )
                )
            }
            dao.updateRide(updatedRide)
        } else {
            dao.updateRide(ride)
        }
    }

    suspend fun deleteRide(ride: RideEntity) = runInTransaction {
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

    suspend fun addParcel(parcel: ParcelEntity): Long = runInTransaction {
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
        val historyDetails = "Parcel to ${parcel.receiverName} (${parcel.deliveryAddress}) Charges: Rs. ${parcel.deliveryCharges.toInt()}, Paid: Rs. ${parcel.amountPaid.toInt()}, Bakaya: Rs. ${parcel.remainingBakaya.toInt()}"
        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerName = parcel.senderName,
                phone = parcel.senderPhone,
                activityType = "PARCEL",
                details = historyDetails,
                amount = parcel.deliveryCharges,
                bakayaAmount = parcel.remainingBakaya,
                timestamp = parcel.date,
                referenceId = parcelId
            )
        )
        parcelId
    }

    suspend fun updateParcel(parcel: ParcelEntity) = runInTransaction {
        val oldParcel = dao.getParcelById(parcel.id)
        if (oldParcel != null) {
            val preservedDate = oldParcel.date
            val updatedParcel = parcel.copy(date = preservedDate)

            val senderChanged = !oldParcel.senderName.equals(updatedParcel.senderName, ignoreCase = true) ||
                    (oldParcel.senderPhone.isNotEmpty() && updatedParcel.senderPhone.isNotEmpty() && oldParcel.senderPhone != updatedParcel.senderPhone)

            if (senderChanged) {
                if (oldParcel.remainingBakaya > 0) {
                    val oldDebtor = dao.findDebtorByNameOrPhone(oldParcel.senderName, oldParcel.senderPhone)
                    if (oldDebtor != null) {
                        val newRemaining = (oldDebtor.remainingDebt - oldParcel.remainingBakaya).coerceAtLeast(0.0)
                        val newTotal = (oldDebtor.totalDebt - oldParcel.remainingBakaya).coerceAtLeast(0.0)
                        dao.updateDebtor(oldDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    }
                }
                if (updatedParcel.remainingBakaya > 0) {
                    val newDebtor = dao.findDebtorByNameOrPhone(updatedParcel.senderName, updatedParcel.senderPhone)
                    if (newDebtor != null) {
                        val newRemaining = newDebtor.remainingDebt + updatedParcel.remainingBakaya
                        val newTotal = newDebtor.totalDebt + updatedParcel.remainingBakaya
                        dao.updateDebtor(newDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    } else {
                        dao.insertDebtor(
                            DebtorEntity(
                                name = updatedParcel.senderName,
                                phone = updatedParcel.senderPhone,
                                totalDebt = updatedParcel.remainingBakaya,
                                remainingDebt = updatedParcel.remainingBakaya,
                                lastUpdated = System.currentTimeMillis(),
                                notes = "Auto-created from edited Parcel #${updatedParcel.id}"
                            )
                        )
                    }
                }
            } else {
                val diffBakaya = updatedParcel.remainingBakaya - oldParcel.remainingBakaya
                if (diffBakaya != 0.0) {
                    val debtor = dao.findDebtorByNameOrPhone(updatedParcel.senderName, updatedParcel.senderPhone)
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
                    } else if (updatedParcel.remainingBakaya > 0) {
                        dao.insertDebtor(
                            DebtorEntity(
                                name = updatedParcel.senderName,
                                phone = updatedParcel.senderPhone,
                                totalDebt = updatedParcel.remainingBakaya,
                                remainingDebt = updatedParcel.remainingBakaya,
                                lastUpdated = System.currentTimeMillis(),
                                notes = "Auto-created from edited Parcel #${updatedParcel.id}"
                            )
                        )
                    }
                }
            }

            val existingHistory = dao.getCustomerHistoryByReference(updatedParcel.id, "PARCEL")
            val historyDetails = "Parcel to ${updatedParcel.receiverName} (${updatedParcel.deliveryAddress}) Charges: Rs. ${updatedParcel.deliveryCharges.toInt()}, Paid: Rs. ${updatedParcel.amountPaid.toInt()}, Bakaya: Rs. ${updatedParcel.remainingBakaya.toInt()}"
            if (existingHistory != null) {
                dao.updateCustomerHistory(
                    existingHistory.copy(
                        customerName = updatedParcel.senderName,
                        phone = updatedParcel.senderPhone,
                        details = historyDetails,
                        amount = updatedParcel.deliveryCharges,
                        bakayaAmount = updatedParcel.remainingBakaya,
                        timestamp = existingHistory.timestamp
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = updatedParcel.senderName,
                        phone = updatedParcel.senderPhone,
                        activityType = "PARCEL",
                        details = historyDetails,
                        amount = updatedParcel.deliveryCharges,
                        bakayaAmount = updatedParcel.remainingBakaya,
                        timestamp = preservedDate,
                        referenceId = updatedParcel.id
                    )
                )
            }
            dao.updateParcel(updatedParcel)
        } else {
            dao.updateParcel(parcel)
        }
    }

    suspend fun deleteParcel(parcel: ParcelEntity) = runInTransaction {
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

    suspend fun addDebtor(debtor: DebtorEntity): Long = runInTransaction {
        val existing = dao.findDebtorByNameOrPhone(debtor.name, debtor.phone)
        val debtorId = if (existing != null) {
            val newTotal = existing.totalDebt + debtor.totalDebt
            val newRemaining = existing.remainingDebt + debtor.remainingDebt
            dao.updateDebtor(
                existing.copy(
                    totalDebt = newTotal,
                    remainingDebt = newRemaining,
                    lastUpdated = System.currentTimeMillis(),
                    notes = if (debtor.notes.isNotBlank()) debtor.notes else existing.notes
                )
            )
            existing.id
        } else {
            dao.insertDebtor(debtor)
        }

        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerName = debtor.name,
                phone = debtor.phone,
                activityType = "BAKAYA",
                details = "Khata added: Rs. ${debtor.remainingDebt.toInt()}${if (debtor.notes.isNotBlank()) " (${debtor.notes})" else ""}",
                amount = debtor.remainingDebt,
                bakayaAmount = debtor.remainingDebt,
                timestamp = System.currentTimeMillis(),
                referenceId = debtorId
            )
        )
        debtorId
    }

    suspend fun updateDebtor(debtor: DebtorEntity) = runInTransaction {
        val oldDebtor = dao.getDebtorById(debtor.id)
        if (oldDebtor != null) {
            val diffRemaining = debtor.remainingDebt - oldDebtor.remainingDebt
            val newTotal = (oldDebtor.totalDebt + diffRemaining).coerceAtLeast(0.0)
            val updatedDebtor = debtor.copy(
                totalDebt = newTotal,
                lastUpdated = System.currentTimeMillis()
            )
            dao.updateDebtor(updatedDebtor)

            if (diffRemaining != 0.0) {
                val adjustmentStr = if (diffRemaining > 0) "+${diffRemaining.toInt()}" else "${diffRemaining.toInt()}"
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = updatedDebtor.name,
                        phone = updatedDebtor.phone,
                        activityType = "BAKAYA",
                        details = "Debtor balance updated: Rs. ${oldDebtor.remainingDebt.toInt()} -> Rs. ${updatedDebtor.remainingDebt.toInt()} (Adjustment: $adjustmentStr)",
                        amount = updatedDebtor.remainingDebt,
                        bakayaAmount = updatedDebtor.remainingDebt,
                        timestamp = System.currentTimeMillis(),
                        referenceId = updatedDebtor.id
                    )
                )
            }
        } else {
            dao.updateDebtor(debtor)
        }
    }

    suspend fun deleteDebtor(debtor: DebtorEntity) = runInTransaction {
        dao.deletePaymentsByDebtorId(debtor.id)
        dao.deleteHistoryForCustomer(debtor.name, debtor.phone)
        dao.deleteDebtor(debtor)
    }

    suspend fun addPayment(debtorId: Long, amount: Double, note: String): Long = runInTransaction {
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
                    details = "Payment of Rs. ${amount.toInt()} received. Remaining Debt: Rs. ${updatedRemaining.toInt()} ($note)",
                    amount = amount,
                    bakayaAmount = updatedRemaining,
                    timestamp = payment.paymentDate,
                    referenceId = paymentId
                )
            )
        }
        paymentId
    }

    suspend fun updatePayment(payment: PaymentHistoryEntity, oldAmount: Double) = runInTransaction {
        val oldPayment = dao.getPaymentById(payment.id)
        val preservedDate = oldPayment?.paymentDate ?: payment.paymentDate
        val updatedPayment = payment.copy(paymentDate = preservedDate)

        val diff = updatedPayment.amountPaid - oldAmount
        val debtor = dao.getDebtorById(updatedPayment.debtorId)
        if (debtor != null) {
            val newRemaining = (debtor.remainingDebt - diff).coerceAtLeast(0.0)
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = newRemaining,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            val existingHistory = dao.getCustomerHistoryByReference(updatedPayment.id, "PAYMENT")
            val historyDetails = "Payment updated: Rs. ${oldAmount.toInt()} -> Rs. ${updatedPayment.amountPaid.toInt()} received. Remaining Debt: Rs. ${newRemaining.toInt()} (${updatedPayment.note})"
            if (existingHistory != null) {
                dao.updateCustomerHistory(
                    existingHistory.copy(
                        amount = updatedPayment.amountPaid,
                        bakayaAmount = newRemaining,
                        details = historyDetails,
                        timestamp = existingHistory.timestamp
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = debtor.name,
                        phone = debtor.phone,
                        activityType = "PAYMENT",
                        details = historyDetails,
                        amount = updatedPayment.amountPaid,
                        bakayaAmount = newRemaining,
                        timestamp = preservedDate,
                        referenceId = updatedPayment.id
                    )
                )
            }
        }
        dao.updatePayment(updatedPayment)
    }

    suspend fun deletePayment(payment: PaymentHistoryEntity) = runInTransaction {
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
        val history = dao.getCustomerHistoryByReference(payment.id, "PAYMENT")
        if (history != null) {
            dao.deleteCustomerHistory(history)
        }
        dao.deletePayment(payment)
    }

    suspend fun updateCustomerBakaya(name: String, phone: String, newBakaya: Double) = runInTransaction {
        val debtor = dao.findDebtorByNameOrPhone(name, phone)
        if (debtor != null) {
            val oldRemaining = debtor.remainingDebt
            val delta = newBakaya - oldRemaining
            val newTotal = (debtor.totalDebt + delta).coerceAtLeast(0.0)
            dao.updateDebtor(
                debtor.copy(
                    remainingDebt = newBakaya,
                    totalDebt = newTotal,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            if (delta != 0.0) {
                val adjustmentStr = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}"
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerName = name,
                        phone = phone,
                        activityType = "BAKAYA",
                        details = "Bakaya updated: Rs. ${oldRemaining.toInt()} -> Rs. ${newBakaya.toInt()} (Adjustment: $adjustmentStr)",
                        amount = newBakaya,
                        bakayaAmount = newBakaya,
                        timestamp = System.currentTimeMillis(),
                        referenceId = debtor.id
                    )
                )
            }
        } else if (newBakaya > 0) {
            val newDebtorId = dao.insertDebtor(
                DebtorEntity(
                    name = name,
                    phone = phone,
                    totalDebt = newBakaya,
                    remainingDebt = newBakaya,
                    lastUpdated = System.currentTimeMillis(),
                    notes = "Added from Customer Ledger"
                )
            )
            dao.insertCustomerHistory(
                CustomerHistoryEntity(
                    customerName = name,
                    phone = phone,
                    activityType = "BAKAYA",
                    details = "Bakaya added: Rs. ${newBakaya.toInt()}",
                    amount = newBakaya,
                    bakayaAmount = newBakaya,
                    timestamp = System.currentTimeMillis(),
                    referenceId = newDebtorId
                )
            )
        }
    }

    suspend fun deleteCustomerBakaya(name: String, phone: String) = runInTransaction {
        val debtor = dao.findDebtorByNameOrPhone(name, phone)
        if (debtor != null) {
            val clearedAmount = debtor.remainingDebt
            val newTotal = (debtor.totalDebt - clearedAmount).coerceAtLeast(0.0)
            dao.updateDebtor(
                debtor.copy(
                    totalDebt = newTotal,
                    remainingDebt = 0.0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            dao.insertCustomerHistory(
                CustomerHistoryEntity(
                    customerName = name,
                    phone = phone,
                    activityType = "BAKAYA",
                    details = "Bakaya cleared (Rs. ${clearedAmount.toInt()} cleared to Rs. 0)",
                    amount = clearedAmount,
                    bakayaAmount = 0.0,
                    timestamp = System.currentTimeMillis(),
                    referenceId = debtor.id
                )
            )
        }
    }

    suspend fun deleteCustomerHistory(history: CustomerHistoryEntity) = runInTransaction {
        val refId = history.referenceId
        when (history.activityType) {
            "PAYMENT" -> {
                if (refId != null && refId > 0) {
                    val payment = dao.getPaymentById(refId)
                    if (payment != null) {
                        val debtor = dao.getDebtorById(payment.debtorId)
                        if (debtor != null) {
                            val restoredRemaining = debtor.remainingDebt + payment.amountPaid
                            dao.updateDebtor(debtor.copy(remainingDebt = restoredRemaining, lastUpdated = System.currentTimeMillis()))
                        }
                        dao.deletePayment(payment)
                    } else {
                        val debtor = dao.findDebtorByNameOrPhone(history.customerName, history.phone)
                        if (debtor != null) {
                            val restoredRemaining = debtor.remainingDebt + history.amount
                            dao.updateDebtor(debtor.copy(remainingDebt = restoredRemaining, lastUpdated = System.currentTimeMillis()))
                        }
                    }
                } else {
                    val debtor = dao.findDebtorByNameOrPhone(history.customerName, history.phone)
                    if (debtor != null) {
                        val restoredRemaining = debtor.remainingDebt + history.amount
                        dao.updateDebtor(debtor.copy(remainingDebt = restoredRemaining, lastUpdated = System.currentTimeMillis()))
                    }
                }
            }
            "BAKAYA", "DEBT_ADDED" -> {
                val debtor = dao.findDebtorByNameOrPhone(history.customerName, history.phone)
                if (debtor != null) {
                    val newRemaining = (debtor.remainingDebt - history.amount).coerceAtLeast(0.0)
                    val newTotal = (debtor.totalDebt - history.amount).coerceAtLeast(0.0)
                    dao.updateDebtor(debtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                }
            }
            "RIDE" -> {
                if (refId != null && refId > 0) {
                    val ride = dao.getRideById(refId)
                    if (ride != null) {
                        if (ride.remainingBakaya > 0) {
                            val debtor = dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
                            if (debtor != null) {
                                val newRemaining = (debtor.remainingDebt - ride.remainingBakaya).coerceAtLeast(0.0)
                                val newTotal = (debtor.totalDebt - ride.remainingBakaya).coerceAtLeast(0.0)
                                dao.updateDebtor(debtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                            }
                        }
                        dao.deleteRide(ride)
                    }
                }
            }
            "PARCEL" -> {
                if (refId != null && refId > 0) {
                    val parcel = dao.getParcelById(refId)
                    if (parcel != null) {
                        if (parcel.remainingBakaya > 0) {
                            val debtor = dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
                            if (debtor != null) {
                                val newRemaining = (debtor.remainingDebt - parcel.remainingBakaya).coerceAtLeast(0.0)
                                val newTotal = (debtor.totalDebt - parcel.remainingBakaya).coerceAtLeast(0.0)
                                dao.updateDebtor(debtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                            }
                        }
                        dao.deleteParcel(parcel)
                    }
                }
            }
        }
        dao.deleteCustomerHistory(history)
    }

    suspend fun deleteCustomerHistoryById(id: Long) = runInTransaction {
        val history = dao.getCustomerHistoryById(id)
        if (history != null) {
            deleteCustomerHistory(history)
        } else {
            dao.deleteCustomerHistoryById(id)
        }
    }

    suspend fun deleteCustomerHistoryBatch(ids: List<Long>) = runInTransaction {
        for (id in ids) {
            deleteCustomerHistoryById(id)
        }
    }

    suspend fun deleteAllCustomerHistory() = runInTransaction {
        dao.clearAllCustomerHistory()
    }

    suspend fun resetAllData() = runInTransaction {
        dao.clearAllRides()
        dao.clearAllParcels()
        dao.clearAllDebtors()
        dao.clearAllPayments()
        dao.clearAllCustomerHistory()
    }
}
