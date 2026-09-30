package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ConfirmationStatus
import com.example.data.entity.TransactionType
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.viewmodel.FinanceViewModel
import java.util.Calendar

enum class AnalyticsRange(val label: String, val days: Int) {
    THIS_MONTH("This Month", 30),
    LAST_3_MONTHS("Last 3 Months", 90),
    LAST_6_MONTHS("Last 6 Months", 180),
    THIS_YEAR("This Year", 365)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val investments by viewModel.investments.collectAsState()
    val loans by viewModel.loans.collectAsState()

    var selectedRange by remember { mutableStateOf(AnalyticsRange.THIS_MONTH) }

    val rangeStartTime = remember(selectedRange) {
        val cal = Calendar.getInstance()
        when (selectedRange) {
            AnalyticsRange.THIS_MONTH -> DateUtils.getStartOfMonth(cal.timeInMillis)
            AnalyticsRange.LAST_3_MONTHS -> { cal.add(Calendar.DAY_OF_YEAR, -90); cal.timeInMillis }
            AnalyticsRange.LAST_6_MONTHS -> { cal.add(Calendar.DAY_OF_YEAR, -180); cal.timeInMillis }
            AnalyticsRange.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                DateUtils.getStartOfDay(cal.timeInMillis)
            }
        }
    }

    val periodTransactions = remember(transactions, rangeStartTime) {
        transactions.filter { it.dateMillis >= rangeStartTime && it.confirmationStatus == ConfirmationStatus.CONFIRMED }
    }

    val totalIncome = periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalSavings = periodTransactions.filter { it.type == TransactionType.SAVINGS }.sumOf { it.amount }
    val totalInvestments = periodTransactions.filter { it.type == TransactionType.INVESTMENT }.sumOf { it.amount }
    val totalLoanPayments = periodTransactions.filter { it.type == TransactionType.LOAN_PAYMENT }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense

    // Category breakdown
    val expenseCategoryMap = remember(periodTransactions) {
        periodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryName }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("analytics_screen"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time range selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsRange.values().forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) }
                        )
                    }
                }
            }

            // Overview Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Cash Flow Overview",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Visual Income vs Expense Bar Chart
                        IncomeExpenseBarChart(
                            income = totalIncome.toFloat(),
                            expense = totalExpense.toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Income", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(totalIncome), fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total Expense", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(totalExpense), fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Net Cash Flow", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    CurrencyFormatter.format(netSavings),
                                    fontWeight = FontWeight.Bold,
                                    color = if (netSavings >= 0) IncomeGreen else ExpenseRed
                                )
                            }
                        }
                    }
                }
            }

            // Key Metrics Breakdown
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Invested in Period", style = MaterialTheme.typography.labelSmall)
                            Text(CurrencyFormatter.format(totalInvestments), fontWeight = FontWeight.Bold, color = InvestmentIndigo)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Loan EMIs Paid", style = MaterialTheme.typography.labelSmall)
                            Text(CurrencyFormatter.format(totalLoanPayments), fontWeight = FontWeight.Bold, color = LoanAmber)
                        }
                    }
                }
            }

            // Category Breakdown Section
            item {
                Text(
                    text = "Expense Categories Breakdown",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (expenseCategoryMap.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "No expenses recorded in this period.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            expenseCategoryMap.forEach { (cat, amount) ->
                                val pct = if (totalExpense > 0) ((amount / totalExpense) * 100).toInt() else 0
                                val progress = if (totalExpense > 0) (amount / totalExpense).toFloat().coerceIn(0f, 1f) else 0f

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${CurrencyFormatter.format(amount)} ($pct%)",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeExpenseBarChart(
    income: Float,
    expense: Float,
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(income, expense, 1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = 46.dp.toPx()
        val spacing = 36.dp.toPx()

        val centerX = width / 2
        val incomeX = centerX - spacing - barWidth / 2
        val expenseX = centerX + spacing - barWidth / 2

        val incomeHeight = (income / maxVal) * (height - 30.dp.toPx())
        val expenseHeight = (expense / maxVal) * (height - 30.dp.toPx())

        val baselineY = height - 20.dp.toPx()

        // Background baseline grid line
        drawLine(
            color = Color.LightGray.copy(alpha = 0.4f),
            start = Offset(20f, baselineY),
            end = Offset(width - 20f, baselineY),
            strokeWidth = 2f
        )

        // Income Bar (Green)
        if (incomeHeight > 0) {
            drawRoundRect(
                color = IncomeGreen,
                topLeft = Offset(incomeX, baselineY - incomeHeight),
                size = Size(barWidth, incomeHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }

        // Expense Bar (Red)
        if (expenseHeight > 0) {
            drawRoundRect(
                color = ExpenseRed,
                topLeft = Offset(expenseX, baselineY - expenseHeight),
                size = Size(barWidth, expenseHeight),
                cornerRadius = CornerRadius(12f, 12f)
            )
        }
    }
}
