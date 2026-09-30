package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,
    val currencySymbol: String = "₹",
    val autoConfirmTrustedSms: Boolean = false,
    val enableSmsDetection: Boolean = false,
    val enableNotificationDetection: Boolean = false,
    val appLockEnabled: Boolean = false,
    val pinHash: String = "",
    val biometricEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val lastBackupDateMillis: Long = 0L
)
