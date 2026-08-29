package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtorId: Long? = null,
    val fromLocation: String,
    val toLocation: String,
    val distanceKm: Double,
    val fareAmount: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val timeString: String = "",
    val note: String = "",
    val imageUri: String? = null
)
