package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debtors")
data class DebtorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val totalDebt: Double,
    val remainingDebt: Double,
    val lastUpdated: Long = System.currentTimeMillis(),
    val notes: String = ""
)
