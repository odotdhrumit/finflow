package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    BANK,
    CASH,
    CREDIT_CARD,
    WALLET,
    OTHER
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val bankName: String = "",
    val accountNumberLast4: String = "",
    val openingBalance: Double = 0.0,
    val currentBalance: Double = 0.0,
    val accountType: AccountType = AccountType.BANK,
    val colorHex: String = "#10B981",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
