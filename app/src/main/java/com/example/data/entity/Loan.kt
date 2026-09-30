package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val lender: String,
    val originalAmount: Double,
    val remainingAmount: Double,
    val interestRate: Double = 0.0,
    val emiAmount: Double,
    val dueDayOfMonth: Int = 1,
    val totalEmis: Int = 12,
    val paidEmis: Int = 0,
    val remainingEmis: Int = 12,
    val frequency: String = "Monthly",
    val notes: String = "",
    val nextDueDateMillis: Long = System.currentTimeMillis(),
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
