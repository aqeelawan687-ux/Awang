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

    fun getCustomerHistoryById(customerId: Long): Flow<List<CustomerHistoryEntity>> =
        dao.getHistoryForCustomerId(customerId)

    fun getRidesForCustomerId(customerId: Long): Flow<List<RideEntity>> =
        dao.getRidesForCustomerId(customerId)

    fun getParcelsForCustomerId(customerId: Long): Flow<List<ParcelEntity>> =
        dao.getParcelsForCustomerId(customerId)

    private suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return if (database != null) {
            database.withTransaction { block() }
        } else {
            block()
        }
    }

    suspend fun addRide(ride: RideEntity): Long = runInTransaction {
        val debtor = if (ride.customerId != null && ride.customerId > 0) {
            dao.getDebtorById(ride.customerId)
        } else {
            dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
        }
        val resolvedCustomerId = if (debtor != null) {
            if (ride.remainingBakaya > 0) {
                dao.updateDebtor(
                    debtor.copy(
                        totalDebt = debtor.totalDebt + ride.remainingBakaya,
                        remainingDebt = debtor.remainingDebt + ride.remainingBakaya,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            debtor.id
        } else {
            dao.insertDebtor(
                DebtorEntity(
                    name = ride.customerName.ifBlank { "Customer" },
                    phone = ride.phone,
                    totalDebt = ride.remainingBakaya,
                    remainingDebt = ride.remainingBakaya,
                    lastUpdated = System.currentTimeMillis(),
                    notes = "Auto-created from Ride"
                )
            )
        }

        val effectiveRide = ride.copy(customerId = resolvedCustomerId)
        val rideId = dao.insertRide(effectiveRide)

        val historyDetails = "Ride from ${ride.pickupLocation} to ${ride.dropoffLocation} (Fare: Rs. ${ride.fare.toInt()}, Paid: Rs. ${ride.amountPaid.toInt()}, Bakaya: Rs. ${ride.remainingBakaya.toInt()})"
        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerId = resolvedCustomerId,
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
            val updatedRide = ride

            val oldDebtorId = oldRide.customerId
            val newDebtorId = updatedRide.customerId
            val customerChanged = (oldDebtorId != null && newDebtorId != null && oldDebtorId != newDebtorId) ||
                    (!oldRide.customerName.equals(updatedRide.customerName, ignoreCase = true)) ||
                    (oldRide.phone.isNotEmpty() && updatedRide.phone.isNotEmpty() && oldRide.phone != updatedRide.phone)

            if (customerChanged) {
                if (oldRide.remainingBakaya > 0) {
                    val oldDebtor = if (oldDebtorId != null) dao.getDebtorById(oldDebtorId) else dao.findDebtorByNameOrPhone(oldRide.customerName, oldRide.phone)
                    if (oldDebtor != null) {
                        val newRemaining = (oldDebtor.remainingDebt - oldRide.remainingBakaya).coerceAtLeast(0.0)
                        val newTotal = (oldDebtor.totalDebt - oldRide.remainingBakaya).coerceAtLeast(0.0)
                        dao.updateDebtor(oldDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    }
                }
                if (updatedRide.remainingBakaya > 0) {
                    val newDebtor = if (newDebtorId != null) dao.getDebtorById(newDebtorId) else dao.findDebtorByNameOrPhone(updatedRide.customerName, updatedRide.phone)
                    if (newDebtor != null) {
                        val newRemaining = newDebtor.remainingDebt + updatedRide.remainingBakaya
                        val newTotal = newDebtor.totalDebt + updatedRide.remainingBakaya
                        dao.updateDebtor(newDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    } else {
                        val insertedId = dao.insertDebtor(
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
                    val debtor = if (updatedRide.customerId != null) dao.getDebtorById(updatedRide.customerId) else dao.findDebtorByNameOrPhone(updatedRide.customerName, updatedRide.phone)
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
                        customerId = updatedRide.customerId ?: existingHistory.customerId,
                        customerName = updatedRide.customerName,
                        phone = updatedRide.phone,
                        details = historyDetails,
                        amount = updatedRide.fare,
                        bakayaAmount = updatedRide.remainingBakaya,
                        timestamp = updatedRide.rideDate
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerId = updatedRide.customerId,
                        customerName = updatedRide.customerName,
                        phone = updatedRide.phone,
                        activityType = "RIDE",
                        details = historyDetails,
                        amount = updatedRide.fare,
                        bakayaAmount = updatedRide.remainingBakaya,
                        timestamp = updatedRide.rideDate,
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
            val debtor = if (ride.customerId != null) dao.getDebtorById(ride.customerId) else dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
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
        val totalCharges = parcel.samanCharges + parcel.deliveryCharges
        val remaining = (totalCharges - parcel.amountPaid).coerceAtLeast(0.0)
        val isPaid = remaining <= 0

        val debtor = if (parcel.customerId != null && parcel.customerId > 0) {
            dao.getDebtorById(parcel.customerId)
        } else {
            dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
        }
        val resolvedCustomerId = if (debtor != null) {
            if (remaining > 0) {
                dao.updateDebtor(
                    debtor.copy(
                        totalDebt = debtor.totalDebt + remaining,
                        remainingDebt = debtor.remainingDebt + remaining,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            debtor.id
        } else {
            dao.insertDebtor(
                DebtorEntity(
                    name = parcel.senderName.ifBlank { "Customer" },
                    phone = parcel.senderPhone,
                    totalDebt = remaining,
                    remainingDebt = remaining,
                    lastUpdated = System.currentTimeMillis(),
                    notes = "Auto-created from Parcel"
                )
            )
        }

        val effectiveParcel = parcel.copy(
            customerId = resolvedCustomerId,
            remainingBakaya = remaining,
            isPaid = isPaid
        )
        val parcelId = dao.insertParcel(effectiveParcel)

        val shopInfo = if (parcel.shopName.isNotBlank()) "Shop: ${parcel.shopName}, " else ""
        val historyDetails = "Parcel to ${parcel.receiverName} (${shopInfo}${parcel.deliveryAddress}) | Saman: Rs. ${parcel.samanCharges.toInt()}, Delivery: Rs. ${parcel.deliveryCharges.toInt()}, Total: Rs. ${totalCharges.toInt()}, Paid: Rs. ${parcel.amountPaid.toInt()}, Bakaya: Rs. ${remaining.toInt()}"
        dao.insertCustomerHistory(
            CustomerHistoryEntity(
                customerId = resolvedCustomerId,
                customerName = parcel.senderName,
                phone = parcel.senderPhone,
                activityType = "PARCEL",
                details = historyDetails,
                amount = totalCharges,
                bakayaAmount = remaining,
                timestamp = parcel.date,
                referenceId = parcelId
            )
        )
        parcelId
    }

    suspend fun updateParcel(parcel: ParcelEntity) = runInTransaction {
        val oldParcel = dao.getParcelById(parcel.id)
        if (oldParcel != null) {
            val totalCharges = parcel.samanCharges + parcel.deliveryCharges
            val remaining = (totalCharges - parcel.amountPaid).coerceAtLeast(0.0)
            val updatedParcel = parcel.copy(
                remainingBakaya = remaining,
                isPaid = remaining <= 0
            )

            val oldDebtorId = oldParcel.customerId
            val newDebtorId = updatedParcel.customerId
            val senderChanged = (oldDebtorId != null && newDebtorId != null && oldDebtorId != newDebtorId) ||
                    (!oldParcel.senderName.equals(updatedParcel.senderName, ignoreCase = true)) ||
                    (oldParcel.senderPhone.isNotEmpty() && updatedParcel.senderPhone.isNotEmpty() && oldParcel.senderPhone != updatedParcel.senderPhone)

            if (senderChanged) {
                if (oldParcel.remainingBakaya > 0) {
                    val oldDebtor = if (oldDebtorId != null) dao.getDebtorById(oldDebtorId) else dao.findDebtorByNameOrPhone(oldParcel.senderName, oldParcel.senderPhone)
                    if (oldDebtor != null) {
                        val newRemaining = (oldDebtor.remainingDebt - oldParcel.remainingBakaya).coerceAtLeast(0.0)
                        val newTotal = (oldDebtor.totalDebt - oldParcel.remainingBakaya).coerceAtLeast(0.0)
                        dao.updateDebtor(oldDebtor.copy(totalDebt = newTotal, remainingDebt = newRemaining, lastUpdated = System.currentTimeMillis()))
                    }
                }
                if (updatedParcel.remainingBakaya > 0) {
                    val newDebtor = if (newDebtorId != null) dao.getDebtorById(newDebtorId) else dao.findDebtorByNameOrPhone(updatedParcel.senderName, updatedParcel.senderPhone)
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
                    val debtor = if (updatedParcel.customerId != null) dao.getDebtorById(updatedParcel.customerId) else dao.findDebtorByNameOrPhone(updatedParcel.senderName, updatedParcel.senderPhone)
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
            val shopInfo = if (updatedParcel.shopName.isNotBlank()) "Shop: ${updatedParcel.shopName}, " else ""
            val historyDetails = "Parcel to ${updatedParcel.receiverName} (${shopInfo}${updatedParcel.deliveryAddress}) | Saman: Rs. ${updatedParcel.samanCharges.toInt()}, Delivery: Rs. ${updatedParcel.deliveryCharges.toInt()}, Total: Rs. ${totalCharges.toInt()}, Paid: Rs. ${updatedParcel.amountPaid.toInt()}, Bakaya: Rs. ${updatedParcel.remainingBakaya.toInt()}"
            if (existingHistory != null) {
                dao.updateCustomerHistory(
                    existingHistory.copy(
                        customerId = updatedParcel.customerId ?: existingHistory.customerId,
                        customerName = updatedParcel.senderName,
                        phone = updatedParcel.senderPhone,
                        details = historyDetails,
                        amount = totalCharges,
                        bakayaAmount = updatedParcel.remainingBakaya,
                        timestamp = updatedParcel.date
                    )
                )
            } else {
                dao.insertCustomerHistory(
                    CustomerHistoryEntity(
                        customerId = updatedParcel.customerId,
                        customerName = updatedParcel.senderName,
                        phone = updatedParcel.senderPhone,
                        activityType = "PARCEL",
                        details = historyDetails,
                        amount = totalCharges,
                        bakayaAmount = updatedParcel.remainingBakaya,
                        timestamp = updatedParcel.date,
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
            val debtor = if (parcel.customerId != null) dao.getDebtorById(parcel.customerId) else dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
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

        if (debtor.remainingDebt > 0) {
            dao.insertCustomerHistory(
                CustomerHistoryEntity(
                    customerId = debtorId,
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
        }
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
                    customerId = debtor.id,
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

    suspend fun recordCustomerPayment(name: String, phone: String, amount: Double, note: String): Long = runInTransaction {
        var debtor = dao.findDebtorByNameOrPhone(name, phone)
        if (debtor == null) {
            val newDebtorId = dao.insertDebtor(
                DebtorEntity(
                    name = name.trim(),
                    phone = phone.trim(),
                    totalDebt = 0.0,
                    remainingDebt = 0.0,
                    lastUpdated = System.currentTimeMillis(),
                    notes = "Customer Account"
                )
            )
            debtor = dao.getDebtorById(newDebtorId)
        }
        if (debtor != null) {
            addPayment(debtor.id, amount, note)
        } else {
            0L
        }
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

    suspend fun updateCustomerBakayaById(customerId: Long, newBakaya: Double) = runInTransaction {
        val debtor = dao.getDebtorById(customerId)
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
                        customerId = debtor.id,
                        customerName = debtor.name,
                        phone = debtor.phone,
                        activityType = "BAKAYA",
                        details = "Bakaya updated: Rs. ${oldRemaining.toInt()} -> Rs. ${newBakaya.toInt()} (Adjustment: $adjustmentStr)",
                        amount = newBakaya,
                        bakayaAmount = newBakaya,
                        timestamp = System.currentTimeMillis(),
                        referenceId = debtor.id
                    )
                )
            }
        }
    }

    suspend fun deleteCustomerBakayaById(customerId: Long) = runInTransaction {
        val debtor = dao.getDebtorById(customerId)
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
                    customerId = debtor.id,
                    customerName = debtor.name,
                    phone = debtor.phone,
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
                        customerId = debtor.id,
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
                    customerId = newDebtorId,
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
                    customerId = debtor.id,
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
                val debtor = if (history.customerId != null) dao.getDebtorById(history.customerId) else dao.findDebtorByNameOrPhone(history.customerName, history.phone)
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
                            val debtor = if (ride.customerId != null) dao.getDebtorById(ride.customerId) else dao.findDebtorByNameOrPhone(ride.customerName, ride.phone)
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
                            val debtor = if (parcel.customerId != null) dao.getDebtorById(parcel.customerId) else dao.findDebtorByNameOrPhone(parcel.senderName, parcel.senderPhone)
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
