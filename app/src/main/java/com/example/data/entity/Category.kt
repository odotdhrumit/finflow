package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "Category",
    val colorHex: String = "#10B981",
    val isIncome: Boolean = false,
    val isDefault: Boolean = false,
    val keywords: String = "" // comma-separated keywords for auto-categorization
)
