package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.FinFlowApplication
import com.example.data.entity.DetectedSourceType
import com.example.parser.SmsTransactionParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {

    private val supportedPackages = setOf(
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "in.org.npci.upiapp", // BHIM
        "com.sbi.lotusintouch", // YONO SBI
        "com.hdfcbank.android",
        "com.csam.icici.bank.imobile",
        "com.axis.mobile",
        "com.msf.kbank.mobile",
        "com.infrasofttech.indianbank",
        "com.canarabank.mobility"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkgName = sbn.packageName ?: ""
        // Check if package is supported bank / UPI or financial app
        val isFinancialApp = supportedPackages.contains(pkgName) ||
                pkgName.contains("bank", ignoreCase = true) ||
                pkgName.contains("pay", ignoreCase = true) ||
                pkgName.contains("upi", ignoreCase = true)

        if (!isFinancialApp) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        val fullContent = "$title $bigText".trim()
        if (fullContent.length < 10) return

        val app = applicationContext as? FinFlowApplication ?: return
        val repository = app.repository

        CoroutineScope(Dispatchers.IO).launch {
            val settings = repository.getSettingsDirect()
            if (!settings.enableNotificationDetection) return@launch

            val parsed = SmsTransactionParser.parse(title, fullContent, sbn.postTime)
            if (parsed != null) {
                val processed = repository.processParsedTransaction(parsed, DetectedSourceType.NOTIFICATION)
                if (processed) {
                    com.example.util.TransactionNotificationManager.showTransactionAlert(
                        context = this@BankNotificationListenerService,
                        amount = parsed.amount,
                        isDebit = parsed.isDebit,
                        bank = parsed.bank,
                        accountLast4 = parsed.accountLast4,
                        merchant = parsed.merchant,
                        balance = parsed.balanceAfterTransaction,
                        upiId = parsed.upiId
                    )
                    com.example.widget.FinFlowWidgetManager.updateAllWidgets(this@BankNotificationListenerService)
                }
            }
        }
    }
}
