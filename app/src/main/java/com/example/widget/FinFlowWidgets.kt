package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.entity.TransactionType
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FinFlow Mega Widget
 * Automatically adapts between Small, Medium, and Large sizes.
 * Operates independently of Activity/ViewModel lifecycle using direct Room DB access.
 */
open class FinFlowMegaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Immediate synchronous safe placeholder so launcher never shows un-inflated state
        for (id in appWidgetIds) {
            try {
                val placeholderViews = FinFlowWidgetManager.buildSafeDefaultViews(context)
                appWidgetManager.updateAppWidget(id, placeholderViews)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Async live data fetch from Room
        FinFlowWidgetManager.updateOverviewWidget(context, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        FinFlowWidgetManager.updateOverviewWidget(context, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                FinFlowWidgetManager.updateAllWidgets(context)
            }
        }
    }
}

/**
 * Retained for backwards-compatibility with any existing widget instances.
 */
class FinancialOverviewWidgetProvider : FinFlowMegaWidgetProvider()

object FinFlowWidgetManager {

    fun updateAllWidgets(context: Context) {
        updateOverviewWidget(context)
    }

    fun buildSafeDefaultViews(context: Context): RemoteViews {
        val appContext = context.applicationContext
        val smallViews = RemoteViews(appContext.packageName, R.layout.widget_overview_small).apply {
            setTextViewText(R.id.widget_small_balance, "₹0")
            setTextViewText(R.id.widget_small_latest, "No transactions yet")
            setOnClickPendingIntent(R.id.widget_small_root, createNavigationIntent(appContext, "dashboard"))
        }
        val mediumViews = RemoteViews(appContext.packageName, R.layout.widget_overview_medium).apply {
            setTextViewText(R.id.widget_med_balance, "₹0")
            setTextViewText(R.id.widget_med_savings, "₹0")
            setTextViewText(R.id.widget_med_investments, "₹0")
            setTextViewText(R.id.widget_med_tx_amount, "No transactions yet")
            setTextViewText(R.id.widget_med_tx_merchant, "")
            setOnClickPendingIntent(R.id.widget_med_root, createNavigationIntent(appContext, "dashboard"))
        }
        val largeViews = RemoteViews(appContext.packageName, R.layout.widget_overview_large).apply {
            setTextViewText(R.id.widget_large_balance, "₹0")
            setTextViewText(R.id.widget_large_savings, "₹0")
            setTextViewText(R.id.widget_large_investments, "₹0")
            setTextViewText(R.id.widget_large_loans, "₹0")
            setTextViewText(R.id.widget_large_tx_amount, "No transactions yet")
            setTextViewText(R.id.widget_large_tx_details, "")
            setTextViewText(R.id.widget_large_emi_amount, "No EMI scheduled")
            setTextViewText(R.id.widget_large_emi_due, "-")
            setTextViewText(R.id.widget_large_emi_days, "")
            setTextViewText(R.id.widget_large_goal_name, "No savings goal")
            setTextViewText(R.id.widget_large_goal_progress, "₹0 / ₹0")
            setTextViewText(R.id.widget_large_goal_pct, "")
            setOnClickPendingIntent(R.id.widget_large_root, createNavigationIntent(appContext, "dashboard"))
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val viewsMap = mapOf(
                SizeF(60f, 60f) to smallViews,
                SizeF(180f, 70f) to mediumViews,
                SizeF(220f, 180f) to largeViews
            )
            RemoteViews(viewsMap)
        } else {
            mediumViews
        }
    }

    fun updateOverviewWidget(context: Context, specificWidgetIds: IntArray? = null) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)

        val megaComponent = ComponentName(appContext, FinFlowMegaWidgetProvider::class.java)
        val legacyComponent = ComponentName(appContext, FinancialOverviewWidgetProvider::class.java)

        val targetWidgetIds = if (specificWidgetIds != null && specificWidgetIds.isNotEmpty()) {
            specificWidgetIds
        } else {
            val megaIds = appWidgetManager.getAppWidgetIds(megaComponent)
            val legacyIds = appWidgetManager.getAppWidgetIds(legacyComponent)
            (megaIds + legacyIds).distinct().toIntArray()
        }

        if (targetWidgetIds.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Direct Room database access independent of Activity / ViewModel
                val database = AppDatabase.getDatabase(appContext)
                val dao = database.financeDao()

                // 1. Total Balance
                val accounts = dao.getActiveAccountsDirect()
                val totalBalance = accounts.sumOf { it.currentBalance }

                // 2. Savings
                val savingsGoals = dao.getAllSavingsGoalsDirect()
                val totalSavings = savingsGoals.sumOf { it.currentAmount }
                val primaryGoal = savingsGoals.firstOrNull()

                // 3. Investments
                val investments = dao.getAllInvestmentsDirect()
                val totalCurrentInvestments = investments.sumOf { it.currentValue }

                // 4. Loans & Next EMI
                val activeLoans = dao.getAllLoansDirect().filter { !it.isClosed }
                val totalLoansDue = activeLoans.sumOf { it.remainingAmount }
                val nextLoan = activeLoans.minByOrNull { it.nextDueDateMillis }

                // 5. Latest Confirmed Transaction
                val latestTx = dao.getAllConfirmedTransactionsDirect().firstOrNull()

                // Transaction Text & Color
                val (txAmountText, txDetailsText, txColorHex) = if (latestTx != null) {
                    val isIncome = latestTx.type == TransactionType.INCOME
                    val prefix = if (isIncome) "+ " else "- "
                    val typeLabel = if (isIncome) "Credit" else "Debit"
                    val formattedAmount = "$prefix${CurrencyFormatter.format(latestTx.amount)} $typeLabel"
                    val details = "${latestTx.merchant.ifBlank { latestTx.categoryName }} • ${DateUtils.formatShortDate(latestTx.dateMillis)}"
                    val color = if (isIncome) 0xFF248255.toInt() else 0xFFC74343.toInt()
                    Triple(formattedAmount, details, color)
                } else {
                    Triple("No transactions yet", "", 0xFF5D546D.toInt())
                }

                // ==================== 1. SMALL WIDGET LAYOUT ====================
                val smallViews = RemoteViews(appContext.packageName, R.layout.widget_overview_small).apply {
                    setTextViewText(R.id.widget_small_balance, CurrencyFormatter.format(totalBalance))
                    setTextViewText(R.id.widget_small_latest, txAmountText)
                    setTextColor(R.id.widget_small_latest, txColorHex)

                    setOnClickPendingIntent(R.id.widget_small_balance_container, createNavigationIntent(appContext, "dashboard"))
                    setOnClickPendingIntent(R.id.widget_small_latest_container, createNavigationIntent(appContext, "transactions"))
                    setOnClickPendingIntent(R.id.widget_small_root, createNavigationIntent(appContext, "dashboard"))
                }

                // ==================== 2. MEDIUM WIDGET LAYOUT ====================
                val mediumViews = RemoteViews(appContext.packageName, R.layout.widget_overview_medium).apply {
                    setTextViewText(R.id.widget_med_balance, CurrencyFormatter.format(totalBalance))
                    setTextViewText(R.id.widget_med_savings, CurrencyFormatter.format(totalSavings))
                    setTextViewText(R.id.widget_med_investments, CurrencyFormatter.format(totalCurrentInvestments))

                    setTextViewText(R.id.widget_med_tx_amount, txAmountText)
                    setTextColor(R.id.widget_med_tx_amount, txColorHex)
                    setTextViewText(
                        R.id.widget_med_tx_merchant,
                        latestTx?.merchant?.ifBlank { latestTx.categoryName } ?: ""
                    )

                    setOnClickPendingIntent(R.id.widget_med_balance_container, createNavigationIntent(appContext, "dashboard"))
                    setOnClickPendingIntent(R.id.widget_med_savings_container, createNavigationIntent(appContext, "savings"))
                    setOnClickPendingIntent(R.id.widget_med_tx_container, createNavigationIntent(appContext, "transactions"))
                    setOnClickPendingIntent(R.id.widget_med_root, createNavigationIntent(appContext, "dashboard"))
                }

                // ==================== 3. LARGE WIDGET LAYOUT ====================
                val largeViews = RemoteViews(appContext.packageName, R.layout.widget_overview_large).apply {
                    setTextViewText(R.id.widget_large_balance, CurrencyFormatter.format(totalBalance))
                    setTextViewText(R.id.widget_large_savings, CurrencyFormatter.format(totalSavings))
                    setTextViewText(R.id.widget_large_investments, CurrencyFormatter.format(totalCurrentInvestments))
                    setTextViewText(R.id.widget_large_loans, CurrencyFormatter.format(totalLoansDue))

                    setTextViewText(R.id.widget_large_tx_amount, txAmountText)
                    setTextColor(R.id.widget_large_tx_amount, txColorHex)
                    setTextViewText(R.id.widget_large_tx_details, txDetailsText)

                    // Next EMI card
                    if (nextLoan != null) {
                        setTextViewText(R.id.widget_large_emi_amount, CurrencyFormatter.format(nextLoan.emiAmount))
                        setTextViewText(R.id.widget_large_emi_due, "Due: ${DateUtils.formatShortDate(nextLoan.nextDueDateMillis)}")
                        setTextViewText(R.id.widget_large_emi_days, DateUtils.getDaysRemainingText(nextLoan.nextDueDateMillis))
                    } else {
                        setTextViewText(R.id.widget_large_emi_amount, "No EMI scheduled")
                        setTextViewText(R.id.widget_large_emi_due, "-")
                        setTextViewText(R.id.widget_large_emi_days, "")
                    }

                    // Savings Goal card
                    if (primaryGoal != null) {
                        val pct = if (primaryGoal.targetAmount > 0) {
                            ((primaryGoal.currentAmount / primaryGoal.targetAmount) * 100).toInt()
                        } else 0
                        setTextViewText(R.id.widget_large_goal_name, primaryGoal.name)
                        setTextViewText(
                            R.id.widget_large_goal_progress,
                            "${CurrencyFormatter.format(primaryGoal.currentAmount)} / ${CurrencyFormatter.format(primaryGoal.targetAmount)}"
                        )
                        setTextViewText(R.id.widget_large_goal_pct, "$pct% complete")
                    } else {
                        setTextViewText(R.id.widget_large_goal_name, "No savings goal")
                        setTextViewText(R.id.widget_large_goal_progress, "₹0 / ₹0")
                        setTextViewText(R.id.widget_large_goal_pct, "")
                    }

                    // Interaction intents
                    setOnClickPendingIntent(R.id.widget_large_balance_container, createNavigationIntent(appContext, "dashboard"))
                    setOnClickPendingIntent(R.id.widget_large_savings_container, createNavigationIntent(appContext, "savings"))
                    setOnClickPendingIntent(R.id.widget_large_loans_container, createNavigationIntent(appContext, "loans"))
                    setOnClickPendingIntent(R.id.widget_large_tx_container, createNavigationIntent(appContext, "transactions"))
                    setOnClickPendingIntent(R.id.widget_large_emi_container, createNavigationIntent(appContext, "loans"))
                    setOnClickPendingIntent(R.id.widget_large_goal_container, createNavigationIntent(appContext, "savings"))
                    setOnClickPendingIntent(R.id.widget_large_root, createNavigationIntent(appContext, "dashboard"))
                }

                // ==================== DISPATCH RESPONSIVE VIEWS ====================
                for (widgetId in targetWidgetIds) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val viewsMap = mapOf(
                            SizeF(60f, 60f) to smallViews,
                            SizeF(180f, 70f) to mediumViews,
                            SizeF(220f, 180f) to largeViews
                        )
                        appWidgetManager.updateAppWidget(widgetId, RemoteViews(viewsMap))
                    } else {
                        val options = appWidgetManager.getAppWidgetOptions(widgetId)
                        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
                        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                        val chosenViews = when {
                            minWidth >= 220 && minHeight >= 180 -> largeViews
                            minWidth >= 180 || minHeight >= 70 -> mediumViews
                            else -> smallViews
                        }
                        appWidgetManager.updateAppWidget(widgetId, chosenViews)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Never crash; fallback to safe views
                try {
                    val fallback = buildSafeDefaultViews(appContext)
                    for (widgetId in targetWidgetIds) {
                        appWidgetManager.updateAppWidget(widgetId, fallback)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun createNavigationIntent(context: Context, destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", destination)
        }
        val requestCode = destination.hashCode()
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
