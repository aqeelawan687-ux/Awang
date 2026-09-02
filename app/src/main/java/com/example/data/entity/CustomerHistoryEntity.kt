package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_history")
data class CustomerHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerName: String,
    val phone: String,
    val activityType: String, // "RIDE", "PARCEL", "PAYMENT", "DEBT_ADDED"
    val details: String,
    val amount: Double,
    val bakayaAmount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: Long? = null
)
