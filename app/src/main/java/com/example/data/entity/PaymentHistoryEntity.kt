package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_history")
data class PaymentHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtorId: Long,
    val debtorName: String,
    val amountPaid: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "Received payment",
    val paymentType: String = "Cash"
)
