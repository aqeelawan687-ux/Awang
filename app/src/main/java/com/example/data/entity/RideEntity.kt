package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerName: String,
    val phone: String,
    val pickupLocation: String,
    val dropoffLocation: String,
    val fare: Double,
    val paymentStatus: String = "PAID", // PAID, UNPAID, PARTIAL
    val amountPaid: Double = fare,
    val remainingBakaya: Double = 0.0,
    val rideDate: Long = System.currentTimeMillis(),
    val notes: String = ""
)
