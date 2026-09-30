package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Account
import com.example.data.entity.Investment
import com.example.data.entity.InvestmentType
import com.example.data.entity.Loan
import com.example.data.entity.SavingsGoal
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.viewmodel.FinanceViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Savings", "Investments", "Loans")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("finance_screen")
    ) {
        Text(
            text = "Finance",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> SavingsTabContent(viewModel)
            1 -> InvestmentsTabContent(viewModel)
            2 -> LoansTabContent(viewModel)
        }
    }
}

// ---------------------- SAVINGS TAB ----------------------
@Composable
fun SavingsTabContent(viewModel: FinanceViewModel) {
    val savingsGoals by viewModel.savingsGoals.collectAsState()
    val accounts by viewModel.activeAccounts.collectAsState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }
    var isDepositMode by remember { mutableStateOf(true) }

    val totalSaved = savingsGoals.sumOf { it.currentAmount }
    val totalTarget = savingsGoals.sumOf { it.targetAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Saved", style = MaterialTheme.typography.labelMedium)
                        Text(
                            CurrencyFormatter.format(totalSaved),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = SavingsTeal
                            )
                        )
                        if (totalTarget > 0) {
                            Text(
                                "Target: ${CurrencyFormatter.format(totalTarget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showAddGoalDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SavingsTeal)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Goal")
                    }
                }
            }
        }

        if (savingsGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = SavingsTeal,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No savings goals yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Set a target for a new gadget, vehicle, or vacation!", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(savingsGoals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val percent = (progress * 100).toInt()

                // Calculate suggested monthly savings
                val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                val remainingMonths = DateUtils.getMonthsDifference(System.currentTimeMillis(), goal.targetDateMillis)
                val suggestedMonthly = remainingAmount / remainingMonths

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = goal.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Target Date: ${DateUtils.formatDate(goal.targetDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { viewModel.deleteSavingsGoal(goal) }) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = SavingsTeal,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${CurrencyFormatter.format(goal.currentAmount)} of ${CurrencyFormatter.format(goal.targetAmount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = SavingsTeal)
                            )
                        }

                        if (remainingAmount > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SavingsTeal.copy(alpha = 0.1f)
                            ) {
                                Text(
                                    text = "Suggested monthly saving: ${CurrencyFormatter.format(suggestedMonthly)} / mo (${remainingMonths} mos left)",
                                    style = MaterialTheme.typography.labelSmall.copy(color = SavingsTeal),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Add Money, Withdraw
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedGoalForDeposit = goal
                                    isDepositMode = false
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Withdraw", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    selectedGoalForDeposit = goal
                                    isDepositMode = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SavingsTeal)
                            ) {
                                Text("Add Money", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetAmountText by remember { mutableStateOf("") }
        var targetMonthsText by remember { mutableStateOf("6") }
        var notesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Create Savings Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Goal Name") },
                        placeholder = { Text("e.g. New Phone, Emergency Fund") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it },
                        label = { Text("Target Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetMonthsText,
                        onValueChange = { targetMonthsText = it },
                        label = { Text("Target Duration (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetAmt = targetAmountText.toDoubleOrNull() ?: 0.0
                        val months = targetMonthsText.toIntOrNull() ?: 6
                        val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, months) }

                        if (goalName.isNotBlank() && targetAmt > 0) {
                            viewModel.addSavingsGoal(
                                name = goalName.trim(),
                                targetAmount = targetAmt,
                                targetDateMillis = targetCal.timeInMillis,
                                notes = notesText.trim(),
                                colorHex = "#14B8A6"
                            )
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Deposit / Withdraw Dialog
    selectedGoalForDeposit?.let { goal ->
        var amountText by remember { mutableStateOf("") }
        var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { selectedGoalForDeposit = null },
            title = { Text(if (isDepositMode) "Add Money to ${goal.name}" else "Withdraw from ${goal.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (accounts.isNotEmpty()) {
                        Text(
                            text = if (isDepositMode) "Deduct from Account:" else "Deposit to Account:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        accounts.forEach { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedAccId == acc.id,
                                    onClick = { selectedAccId = acc.id }
                                )
                                Text("${acc.name} (${CurrencyFormatter.format(acc.currentBalance)})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            if (isDepositMode) {
                                viewModel.addMoneyToSavings(goal.id, amt, selectedAccId)
                            } else {
                                viewModel.withdrawMoneyFromSavings(goal.id, amt, selectedAccId)
                            }
                            selectedGoalForDeposit = null
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedGoalForDeposit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ---------------------- INVESTMENTS TAB ----------------------
@Composable
fun InvestmentsTabContent(viewModel: FinanceViewModel) {
    val investments by viewModel.investments.collectAsState()
    val accounts by viewModel.activeAccounts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<Investment?>(null) }

    val totalInvested = investments.sumOf { it.investedAmount }
    val totalCurrent = investments.sumOf { it.currentValue }
    val totalProfitLoss = totalCurrent - totalInvested
    val plPercent = if (totalInvested > 0) ((totalProfitLoss / totalInvested) * 100) else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Value", style = MaterialTheme.typography.labelMedium)
                            Text(
                                CurrencyFormatter.format(totalCurrent),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = InvestmentIndigo
                                )
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InvestmentIndigo)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Invested", style = MaterialTheme.typography.labelSmall)
                            Text(CurrencyFormatter.format(totalInvested), fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Profit / Loss", style = MaterialTheme.typography.labelSmall)
                            val plColor = if (totalProfitLoss >= 0) IncomeGreen else ExpenseRed
                            val sign = if (totalProfitLoss >= 0) "+" else ""
                            Text(
                                "$sign${CurrencyFormatter.format(totalProfitLoss)} (${String.format("%.2f", plPercent)}%)",
                                fontWeight = FontWeight.Bold,
                                color = plColor
                            )
                        }
                    }
                }
            }
        }

        if (investments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = InvestmentIndigo,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No investments added yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Track Mutual Funds, Stocks, Gold, FDs, RDs with profit/loss", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(investments, key = { it.id }) { inv ->
                val pl = inv.currentValue - inv.investedAmount
                val plPct = if (inv.investedAmount > 0) ((pl / inv.investedAmount) * 100) else 0.0
                val plColor = if (pl >= 0) IncomeGreen else ExpenseRed

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(inv.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = InvestmentIndigo.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = inv.type.name.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall.copy(color = InvestmentIndigo),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (inv.notes.isNotBlank()) {
                                    Text(inv.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row {
                                IconButton(onClick = { editingInvestment = inv }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteInvestment(inv) }) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Invested", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(inv.investedAmount), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Current Value", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(inv.currentValue), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("P/L", style = MaterialTheme.typography.labelSmall)
                                val sign = if (pl >= 0) "+" else ""
                                Text(
                                    "$sign${CurrencyFormatter.format(pl)} (${String.format("%.1f", plPct)}%)",
                                    fontWeight = FontWeight.Bold,
                                    color = plColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Investment Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var type by remember { mutableStateOf(InvestmentType.MUTUAL_FUND) }
        var investedAmountText by remember { mutableStateOf("") }
        var currentValueText by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var deductAccountId by remember { mutableStateOf<Long?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Investment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Investment Name") },
                        placeholder = { Text("e.g. Nifty 50 Index, Apple Inc, Gold") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InvestmentType.values().take(3).forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.name.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InvestmentType.values().drop(3).forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t },
                                label = { Text(t.name.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = investedAmountText,
                        onValueChange = {
                            investedAmountText = it
                            if (currentValueText.isEmpty()) currentValueText = it
                        },
                        label = { Text("Invested Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentValueText,
                        onValueChange = { currentValueText = it },
                        label = { Text("Current Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (accounts.isNotEmpty()) {
                        Text("Deduct from account (optional):", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            accounts.take(3).forEach { acc ->
                                FilterChip(
                                    selected = deductAccountId == acc.id,
                                    onClick = { deductAccountId = if (deductAccountId == acc.id) null else acc.id },
                                    label = { Text(acc.name, fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val invested = investedAmountText.toDoubleOrNull() ?: 0.0
                        val current = currentValueText.toDoubleOrNull() ?: invested
                        if (name.isNotBlank() && invested > 0) {
                            viewModel.addInvestment(name.trim(), type, invested, current, deductAccountId, notes.trim())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Investment Dialog (Update Current Value)
    editingInvestment?.let { inv ->
        var newCurrentValue by remember { mutableStateOf(inv.currentValue.toString()) }

        AlertDialog(
            onDismissRequest = { editingInvestment = null },
            title = { Text("Update Value for ${inv.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Invested: ${CurrencyFormatter.format(inv.investedAmount)}")
                    OutlinedTextField(
                        value = newCurrentValue,
                        onValueChange = { newCurrentValue = it },
                        label = { Text("Current Market Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newCurrentValue.toDoubleOrNull()
                        if (parsed != null) {
                            viewModel.updateInvestment(inv.copy(currentValue = parsed))
                            editingInvestment = null
                        }
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingInvestment = null }) { Text("Cancel") }
            }
        )
    }
}

// ---------------------- LOANS TAB ----------------------
@Composable
fun LoansTabContent(viewModel: FinanceViewModel) {
    val loans by viewModel.loans.collectAsState()
    val accounts by viewModel.activeAccounts.collectAsState()

    var showAddLoanDialog by remember { mutableStateOf(false) }
    var selectedLoanForPayment by remember { mutableStateOf<Loan?>(null) }

    val totalRemaining = loans.filter { !it.isClosed }.sumOf { it.remainingAmount }
    val totalEmisDue = loans.filter { !it.isClosed }.sumOf { it.emiAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Outstanding Loan", style = MaterialTheme.typography.labelMedium)
                            Text(
                                CurrencyFormatter.format(totalRemaining),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = LoanAmber
                                )
                            )
                        }

                        Button(
                            onClick = { showAddLoanDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LoanAmber)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Loan")
                        }
                    }

                    if (totalEmisDue > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Monthly EMI commitments: ${CurrencyFormatter.format(totalEmisDue)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (loans.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = LoanAmber,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No loans tracked yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Track Personal, Home, Car, or Education loans and EMIs", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        } else {
            items(loans, key = { it.id }) { loan ->
                val progress = if (loan.totalEmis > 0) (loan.paidEmis.toFloat() / loan.totalEmis.toFloat()) else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(loan.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    if (loan.isClosed) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(shape = RoundedCornerShape(6.dp), color = IncomeGreen.copy(alpha = 0.2f)) {
                                            Text("CLOSED", style = MaterialTheme.typography.labelSmall.copy(color = IncomeGreen), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("Lender: ${loan.lender}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(onClick = { viewModel.deleteLoan(loan) }) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Remaining", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(loan.remainingAmount), fontWeight = FontWeight.Bold, color = LoanAmber)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("EMI Amount", style = MaterialTheme.typography.labelSmall)
                                Text(CurrencyFormatter.format(loan.emiAmount), fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Next Due", style = MaterialTheme.typography.labelSmall)
                                Text(DateUtils.formatDate(loan.nextDueDateMillis), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // EMI Progress bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = LoanAmber,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${loan.paidEmis} of ${loan.totalEmis} EMIs paid", style = MaterialTheme.typography.labelSmall)
                            Text("${loan.remainingEmis} remaining", style = MaterialTheme.typography.labelSmall)
                        }

                        if (!loan.isClosed) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { selectedLoanForPayment = loan },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LoanAmber)
                            ) {
                                Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark EMI as Paid (${CurrencyFormatter.format(loan.emiAmount)})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Loan Dialog
    if (showAddLoanDialog) {
        var loanName by remember { mutableStateOf("") }
        var lender by remember { mutableStateOf("") }
        var originalAmountText by remember { mutableStateOf("") }
        var remainingAmountText by remember { mutableStateOf("") }
        var interestRateText by remember { mutableStateOf("10.5") }
        var emiAmountText by remember { mutableStateOf("") }
        var totalEmisText by remember { mutableStateOf("12") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = { Text("Add Loan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = loanName,
                        onValueChange = { loanName = it },
                        label = { Text("Loan Name") },
                        placeholder = { Text("e.g. Home Loan, Personal Loan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = lender,
                        onValueChange = { lender = it },
                        label = { Text("Lender / Bank") },
                        placeholder = { Text("e.g. SBI, HDFC, Bajaj Finserv") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = originalAmountText,
                            onValueChange = {
                                originalAmountText = it
                                if (remainingAmountText.isEmpty()) remainingAmountText = it
                            },
                            label = { Text("Original (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = remainingAmountText,
                            onValueChange = { remainingAmountText = it },
                            label = { Text("Remaining (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emiAmountText,
                            onValueChange = { emiAmountText = it },
                            label = { Text("EMI (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalEmisText,
                            onValueChange = { totalEmisText = it },
                            label = { Text("Total EMIs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it },
                        label = { Text("Interest Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val original = originalAmountText.toDoubleOrNull() ?: 0.0
                        val remaining = remainingAmountText.toDoubleOrNull() ?: original
                        val emi = emiAmountText.toDoubleOrNull() ?: (remaining / (totalEmisText.toIntOrNull() ?: 12))
                        val totalEmis = totalEmisText.toIntOrNull() ?: 12
                        val interest = interestRateText.toDoubleOrNull() ?: 0.0

                        if (loanName.isNotBlank() && remaining > 0 && emi > 0) {
                            viewModel.addLoan(
                                name = loanName.trim(),
                                lender = lender.trim().ifBlank { "Bank" },
                                originalAmount = original,
                                remainingAmount = remaining,
                                interestRate = interest,
                                emiAmount = emi,
                                dueDayOfMonth = 10,
                                totalEmis = totalEmis,
                                notes = notes.trim()
                            )
                            showAddLoanDialog = false
                        }
                    }
                ) {
                    Text("Save Loan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLoanDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Mark EMI as Paid Dialog
    selectedLoanForPayment?.let { loan ->
        var selectedAccId by remember { mutableStateOf(accounts.firstOrNull()?.id) }

        AlertDialog(
            onDismissRequest = { selectedLoanForPayment = null },
            title = { Text("Record EMI Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Loan: ${loan.name} (${loan.lender})")
                    Text("EMI Amount: ${CurrencyFormatter.format(loan.emiAmount)}", fontWeight = FontWeight.Bold, color = LoanAmber)
                    Text("This will deduct ${CurrencyFormatter.format(loan.emiAmount)} from selected account, reduce remaining loan balance, and advance the EMI count.")

                    if (accounts.isNotEmpty()) {
                        Text("Deduct from Account:", style = MaterialTheme.typography.labelSmall)
                        accounts.forEach { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedAccId == acc.id,
                                    onClick = { selectedAccId = acc.id }
                                )
                                Text("${acc.name} (${CurrencyFormatter.format(acc.currentBalance)})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val accId = selectedAccId ?: accounts.firstOrNull()?.id ?: return@Button
                        viewModel.payLoanEmi(loan.id, loan.emiAmount, accId)
                        selectedLoanForPayment = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LoanAmber)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedLoanForPayment = null }) { Text("Cancel") }
            }
        )
    }
}
