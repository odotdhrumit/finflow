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
import com.example.util.TransactionNotificationManager
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
                    TransactionNotificationManager.showTransactionAlert(
                        context = context,
                        amount = parsed.amount,
                        isDebit = parsed.isDebit,
                        bank = parsed.bank,
                        accountLast4 = parsed.accountLast4,
                        merchant = parsed.merchant,
                        balance = parsed.balanceAfterTransaction,
                        upiId = parsed.upiId
                    )
                    com.example.widget.FinFlowWidgetManager.updateAllWidgets(context)
                }
            }
        }
    }
}
