package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import com.example.FinFlowApplication
import com.example.MainActivity
import com.example.R
import com.example.data.entity.DetectedSourceType
import com.example.parser.SmsTransactionParser
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as? FinFlowApplication ?: return
        val repository = app.repository

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: "Unknown"
        val fullBody = StringBuilder()
        var timestamp = System.currentTimeMillis()

        for (sms in messages) {
            fullBody.append(sms.messageBody)
            timestamp = sms.timestampMillis
        }

        val messageText = fullBody.toString()

        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getSettingsDirect()
            if (!settings.enableSmsDetection) {
                return@launch
            }

            val parsed = SmsTransactionParser.parse(sender, messageText, timestamp)
            if (parsed != null) {
                val processed = repository.processParsedTransaction(parsed, DetectedSourceType.SMS)
                if (processed) {
                    showNotification(context, parsed.bank, parsed.amount, parsed.isDebit, parsed.merchant)
                }
            }
        }
    }

    private fun showNotification(context: Context, bank: String, amount: Double, isDebit: Boolean, merchant: String) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "sms_detection")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmt = CurrencyFormatter.format(amount)
        val actionText = if (isDebit) "Debited" else "Credited"

        val notification = NotificationCompat.Builder(context, FinFlowApplication.CHANNEL_DETECTIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transaction Detected ($bank)")
            .setContentText("$formattedAmt $actionText at $merchant")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$formattedAmt $actionText from $bank at $merchant. Tap to review."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
