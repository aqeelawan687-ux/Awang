package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debtors")
data class DebtorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val totalDebt: Double,
    val note: String = "",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
