package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.repository.FinanceRepository
import com.example.parser.CategoryKeywordMatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FinFlowApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: FinanceRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = FinanceRepository(database.financeDao())

        createNotificationChannels()
        initializeDefaultData()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Dedicated High Priority Transaction Alerts Channel (Heads-up notification)
            val transactionAlertsChannel = NotificationChannel(
                CHANNEL_TRANSACTION_ALERTS,
                "ORYVO Transaction Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Immediate heads-up alerts for credited income and debited expenses"
                enableVibration(true)
            }

            // EMI Reminders Channel
            val emiChannel = NotificationChannel(
                CHANNEL_EMI_REMINDERS,
                "ORYVO EMI Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming and due loan EMIs"
                enableVibration(true)
            }

            // Savings Reminders Channel
            val savingsChannel = NotificationChannel(
                CHANNEL_SAVINGS_REMINDERS,
                "ORYVO Savings Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders and milestones for your savings goals"
            }

            // Legacy Reminders channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming loan EMIs and bill payment reminders"
                enableVibration(true)
            }

            // Legacy Transaction detection channel
            val detectionChannel = NotificationChannel(
                CHANNEL_DETECTIONS,
                "Transaction Detection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for newly detected bank transactions from SMS and apps"
            }

            notificationManager.createNotificationChannel(transactionAlertsChannel)
            notificationManager.createNotificationChannel(emiChannel)
            notificationManager.createNotificationChannel(savingsChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(detectionChannel)
        }
    }

    private fun initializeDefaultData() {
        CoroutineScope(Dispatchers.IO).launch {
            val categories = database.financeDao().getAllCategoriesDirect()
            if (categories.isEmpty()) {
                database.financeDao().insertCategories(CategoryKeywordMatcher.getDefaultCategories())
            }
            // Do NOT add demo data on app open; cleanly remove any sample data so user starts fresh
            repository.removeDemoDataIfPresent()

            val settings = database.financeDao().getSettingsDirect()
            if (settings == null) {
                database.financeDao().saveSettings(
                    com.example.data.entity.AppSettings(
                        id = 1,
                        hasCompletedOnboarding = true
                    )
                )
            }
        }
    }

    companion object {
        const val CHANNEL_TRANSACTION_ALERTS = "finflow_transaction_alerts"
        const val CHANNEL_EMI_REMINDERS = "finflow_emi_reminders"
        const val CHANNEL_SAVINGS_REMINDERS = "finflow_savings_reminders"
        const val CHANNEL_REMINDERS = "finflow_reminders"
        const val CHANNEL_DETECTIONS = "finflow_detections"
    }
}
