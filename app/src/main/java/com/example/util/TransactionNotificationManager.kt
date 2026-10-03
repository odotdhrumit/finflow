package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.FinFlowApplication
import com.example.MainActivity

object TransactionNotificationManager {

    fun showTransactionAlert(
        context: Context,
        amount: Double,
        isDebit: Boolean,
        bank: String?,
        accountLast4: String?,
        merchant: String?,
        balance: Double?,
        upiId: String? = null,
        transactionId: Long? = null
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val formattedAmt = CurrencyFormatter.format(amount)
        val actionText = if (isDebit) "debited" else "credited"
        val title = if (isDebit) "Money Spent • $formattedAmt" else "Money Received • $formattedAmt"

        val accountText = if (!accountLast4.isNullOrBlank()) "Account ****$accountLast4" else (bank ?: "Account")
        val shortContent = "$formattedAmt $actionText ($accountText)"

        // Build rich detail summary
        val bigTextBuilder = StringBuilder()
        bigTextBuilder.append("$formattedAmt $actionText to your account\n")

        val partyLabel = if (isDebit) "At: " else "From: "
        if (!merchant.isNullOrBlank() && !merchant.equals("Bank", ignoreCase = true)) {
            bigTextBuilder.append("$partyLabel$merchant\n")
        }
        if (!upiId.isNullOrBlank()) {
            bigTextBuilder.append("UPI: $upiId\n")
        }
        if (!accountLast4.isNullOrBlank()) {
            bigTextBuilder.append("Account: ****$accountLast4\n")
        }
        if (balance != null && balance > 0.0) {
            bigTextBuilder.append("Available Balance: ${CurrencyFormatter.format(balance)}")
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "transactions")
            if (transactionId != null && transactionId > 0) {
                putExtra("TRANSACTION_ID", transactionId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 100000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FinFlowApplication.CHANNEL_TRANSACTION_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigTextBuilder.toString().trim()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
