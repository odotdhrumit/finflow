package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DetectedSourceType {
    SMS,
    NOTIFICATION
}

enum class DetectedStatus {
    PENDING_REVIEW,
    CONFIRMED,
    IGNORED,
    FAILED_PARSING
}

@Entity(tableName = "detected_messages")
data class DetectedMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceType: DetectedSourceType = DetectedSourceType.SMS,
    val senderOrApp: String,
    val rawText: String,
    val detectedAtMillis: Long = System.currentTimeMillis(),
    val parsedAmount: Double? = null,
    val parsedType: TransactionType? = null,
    val parsedBank: String? = null,
    val parsedAccountLast4: String? = null,
    val parsedMerchant: String? = null,
    val suggestedCategory: String? = null,
    val status: DetectedStatus = DetectedStatus.PENDING_REVIEW,
    val duplicateFingerprint: String = "",
    val linkedTransactionId: Long? = null
)
