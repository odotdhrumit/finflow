package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class InvestmentType {
    MUTUAL_FUND,
    STOCKS,
    GOLD,
    FD,
    RD,
    OTHER
}

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: InvestmentType = InvestmentType.MUTUAL_FUND,
    val investedAmount: Double,
    val currentValue: Double,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
