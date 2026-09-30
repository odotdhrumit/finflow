package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReminderFrequency {
    ONE_TIME,
    MONTHLY,
    CUSTOM
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double = 0.0,
    val dueDateMillis: Long,
    val frequency: ReminderFrequency = ReminderFrequency.MONTHLY,
    val reminderAdvanceDays: Int = 1, // e.g. 1 day before, 3 days before
    val isCompleted: Boolean = false,
    val linkedLoanId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
