package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parcels")
data class ParcelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtorId: Long? = null,
    val shopName: String,
    val recipientName: String,
    val recipientAddress: String,
    val itemDetails: String,
    val itemPrice: Double,
    val deliveryCharges: Double,
    val isDelivered: Boolean = false,
    val isPaid: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val deliveredTimestamp: Long? = null
)
