package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parcels")
data class ParcelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long? = null,
    val senderName: String,
    val senderPhone: String,
    val receiverName: String,
    val receiverPhone: String,
    val pickupAddress: String,
    val deliveryAddress: String,
    val shopName: String = "",
    val samanCharges: Double = 0.0,
    val deliveryCharges: Double = 0.0,
    val isDelivered: Boolean = false,
    val isPaid: Boolean = false,
    val amountPaid: Double = 0.0,
    val remainingBakaya: Double = (samanCharges + deliveryCharges) - amountPaid,
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val totalCharges: Double get() = samanCharges + deliveryCharges
}
