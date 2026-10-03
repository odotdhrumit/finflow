package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.FinFlowApplication
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.entity.ReminderFrequency
import com.example.util.CurrencyFormatter
import com.example.util.ReminderScheduler
import com.example.widget.FinFlowWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext

        // Handle reboot: reschedule all active reminders and update widget
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(appContext).financeDao()
                    val activeReminders = dao.getActiveRemindersDirect()
                    for (reminder in activeReminders) {
                        ReminderScheduler.scheduleReminder(appContext, reminder)
                    }
                    FinFlowWidgetManager.updateAllWidgets(appContext)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return
        }

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Payment Reminder"
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "reminders")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountStr = if (amount > 0) " of ${CurrencyFormatter.format(amount)}" else ""
        val contentText = "$title$amountStr is due. Tap to view details."

        val channelId = if (title.contains("EMI", ignoreCase = true) || title.contains("Loan", ignoreCase = true)) {
            FinFlowApplication.CHANNEL_EMI_REMINDERS
        } else {
            FinFlowApplication.CHANNEL_REMINDERS
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Payment Due: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reminderId.toInt(), notification)

        // Automatically reschedule next month's reminder for recurring monthly reminders (EMI)
        if (reminderId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(appContext).financeDao()
                    val reminder = dao.getReminderById(reminderId)
                    if (reminder != null && reminder.frequency == ReminderFrequency.MONTHLY && !reminder.isCompleted) {
                        val nextMonthCal = Calendar.getInstance().apply {
                            timeInMillis = reminder.dueDateMillis
                            add(Calendar.MONTH, 1)
                        }
                        val updatedReminder = reminder.copy(dueDateMillis = nextMonthCal.timeInMillis)
                        dao.updateReminder(updatedReminder)
                        ReminderScheduler.scheduleReminder(appContext, updatedReminder)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_AMOUNT = "extra_amount"
    }
}
