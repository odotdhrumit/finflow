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

            // Reminders channel
            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming loan EMIs and bill payment reminders"
                enableVibration(true)
            }

            // Transaction detection channel
            val detectionChannel = NotificationChannel(
                CHANNEL_DETECTIONS,
                "Transaction Detection",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts for newly detected bank transactions from SMS and apps"
            }

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
        }
    }

    companion object {
        const val CHANNEL_REMINDERS = "finflow_reminders"
        const val CHANNEL_DETECTIONS = "finflow_detections"
    }
}
