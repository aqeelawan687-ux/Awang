package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_history")
data class CustomerHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phoneNumber: String = "",
    val actionType: String, // "Ride", "Parcel", "Payment", "Account", etc.
    val title: String,
    val details: String,
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)
