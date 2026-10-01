package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.entity.AccountType
import com.example.data.entity.ConfirmationStatus
import com.example.data.entity.DetectedSourceType
import com.example.data.entity.DetectedStatus
import com.example.data.entity.InvestmentType
import com.example.data.entity.ReminderFrequency
import com.example.data.entity.TransactionSource
import com.example.data.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let { AccountType.valueOf(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { TransactionType.valueOf(it) }

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource?): String? = value?.name

    @TypeConverter
    fun toTransactionSource(value: String?): TransactionSource? = try {
        value?.let { TransactionSource.valueOf(it) }
    } catch (e: Exception) {
        TransactionSource.MANUAL
    }

    @TypeConverter
    fun fromConfirmationStatus(value: ConfirmationStatus?): String? = value?.name

    @TypeConverter
    fun toConfirmationStatus(value: String?): ConfirmationStatus? = value?.let { ConfirmationStatus.valueOf(it) }

    @TypeConverter
    fun fromInvestmentType(value: InvestmentType?): String? = value?.name

    @TypeConverter
    fun toInvestmentType(value: String?): InvestmentType? = value?.let { InvestmentType.valueOf(it) }

    @TypeConverter
    fun fromReminderFrequency(value: ReminderFrequency?): String? = value?.name

    @TypeConverter
    fun toReminderFrequency(value: String?): ReminderFrequency? = value?.let { ReminderFrequency.valueOf(it) }

    @TypeConverter
    fun fromDetectedSourceType(value: DetectedSourceType?): String? = value?.name

    @TypeConverter
    fun toDetectedSourceType(value: String?): DetectedSourceType? = value?.let { DetectedSourceType.valueOf(it) }

    @TypeConverter
    fun fromDetectedStatus(value: DetectedStatus?): String? = value?.name

    @TypeConverter
    fun toDetectedStatus(value: String?): DetectedStatus? = value?.let { DetectedStatus.valueOf(it) }
}
