package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val tabs = listOf("Savings", "Investments", "Loans & EMI")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("finance_screen")
    ) {
        Text(
            text = "Finance",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var isDepositMode by remember { mutableStateOf(true) }

    val totalSaved = savingsGoals.sumOf { it.currentAmount }
    val totalTarget = savingsGoals.sumOf { it.targetAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "TOTAL SAVED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            CurrencyFormatter.format(totalSaved),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
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
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Goal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (savingsGoals.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No savings goals", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Create your first savings goal to track progress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(savingsGoals, key = { it.id }) { goal ->
                val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
                val percent = (progress * 100).toInt()
                val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                val remainingMonths = DateUtils.getMonthsDifference(System.currentTimeMillis(), goal.targetDateMillis)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )

                            Row {
                                IconButton(onClick = { editingGoal = goal }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Goal", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteSavingsGoal(goal) }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "${CurrencyFormatter.format(goal.currentAmount)} saved",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = "of ${CurrencyFormatter.format(goal.targetAmount)} target",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (percent >= 100) PositiveGreen else MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Clean Navy Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${CurrencyFormatter.format(remainingAmount)} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (remainingAmount > 0) {
                                Text(
                                    text = "${remainingMonths} mos left",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Add Money", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
            title = { Text("Create Savings Goal", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Goal Name") },
                        placeholder = { Text("e.g. Emergency Fund, Vacation") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetAmountText,
                        onValueChange = { targetAmountText = it },
                        label = { Text("Target Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetMonthsText,
                        onValueChange = { targetMonthsText = it },
                        label = { Text("Target Duration (Months)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                                colorHex = "#17324D"
                            )
                            showAddGoalDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

    // Edit Savings Goal Dialog
    editingGoal?.let { goal ->
        var editName by remember(goal) { mutableStateOf(goal.name) }
        var editTargetText by remember(goal) { mutableStateOf(goal.targetAmount.toInt().toString()) }
        var editNotes by remember(goal) { mutableStateOf(goal.notes) }

        AlertDialog(
            onDismissRequest = { editingGoal = null },
            title = { Text("Edit Savings Goal", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Goal Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTargetText,
                        onValueChange = { editTargetText = it },
                        label = { Text("Target Amount (₹)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTarget = editTargetText.toDoubleOrNull() ?: goal.targetAmount
                        if (editName.isNotBlank() && newTarget > 0) {
                            viewModel.updateSavingsGoal(
                                goal.copy(
                                    name = editName.trim(),
                                    targetAmount = newTarget,
                                    notes = editNotes.trim()
                                )
                            )
                            editingGoal = null
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGoal = null }) {
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
            title = { Text(if (isDepositMode) "Add Money to ${goal.name}" else "Withdraw from ${goal.name}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                                Text("${acc.name} (${CurrencyFormatter.format(acc.currentBalance)})", style = MaterialTheme.typography.bodyMedium)
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
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "PORTFOLIO VALUE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                CurrencyFormatter.format(totalCurrent),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(CurrencyFormatter.format(totalInvested), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gain / Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val plColor = if (totalProfitLoss >= 0) PositiveGreen else NegativeRed
                            val sign = if (totalProfitLoss >= 0) "+" else ""
                            Text(
                                "$sign${CurrencyFormatter.format(totalProfitLoss)} (${String.format("%.2f", plPercent)}%)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = plColor
                                )
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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No investments", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add your investments to track your portfolio.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(investments, key = { it.id }) { inv ->
                val pl = inv.currentValue - inv.investedAmount
                val plPct = if (inv.investedAmount > 0) ((pl / inv.investedAmount) * 100) else 0.0
                val plColor = if (pl >= 0) PositiveGreen else NegativeRed

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                                    Text(inv.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = inv.type.name.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (inv.notes.isNotBlank()) {
                                    Text(inv.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Row {
                                IconButton(onClick = { editingInvestment = inv }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { viewModel.deleteInvestment(inv) }, modifier = Modifier.size(32.dp)) {
                                    Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = NegativeRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Invested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.format(inv.investedAmount), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Current Value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.format(inv.currentValue), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Gain / Loss", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val sign = if (pl >= 0) "+" else ""
                                Text(
                                    "$sign${CurrencyFormatter.format(pl)} (${String.format("%.1f", plPct)}%)",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = plColor
                                    )
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
            title = { Text("Add Investment", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Investment Name") },
                        placeholder = { Text("e.g. Nifty 50 Index, Apple, Gold ETF") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.fillMaxWidth(),
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
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentValueText,
                        onValueChange = { currentValueText = it },
                        label = { Text("Current Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            title = { Text("Update Value for ${inv.name}", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Invested: ${CurrencyFormatter.format(inv.investedAmount)}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = newCurrentValue,
                        onValueChange = { newCurrentValue = it },
                        label = { Text("Current Market Value (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
@OptIn(ExperimentalMaterial3Api::class)
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "TOTAL OUTSTANDING LOANS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                CurrencyFormatter.format(totalRemaining),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Button(
                            onClick = { showAddLoanDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Loan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (totalEmisDue > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Monthly commitments: ${CurrencyFormatter.format(totalEmisDue)}",
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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No active loans", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add a loan to track EMIs and upcoming payment due dates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(loans, key = { it.id }) { loan ->
                val progress = if (loan.totalEmis > 0) (loan.paidEmis.toFloat() / loan.totalEmis.toFloat()) else 0f

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                                    Text(loan.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                                    if (loan.isClosed) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(shape = RoundedCornerShape(6.dp), color = PositiveGreenLight) {
                                            Text("CLOSED", style = MaterialTheme.typography.labelSmall.copy(color = PositiveGreen, fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("Lender: ${loan.lender}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            IconButton(onClick = { viewModel.deleteLoan(loan) }, modifier = Modifier.size(32.dp)) {
                                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.format(loan.remainingAmount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("EMI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.format(loan.emiAmount), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Next Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(DateUtils.formatFullDate(loan.nextDueDateMillis), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                            }
                        }

                        if (!loan.isClosed) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val dueStatus = DateUtils.getDueStatus(loan.nextDueDateMillis)
                            val isDueTodayOrOverdue = dueStatus.contains("Today") || dueStatus.contains("Overdue")
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDueTodayOrOverdue) NegativeRedLight else WarningAmberLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = null,
                                        tint = if (isDueTodayOrOverdue) NegativeRed else WarningAmber,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = dueStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDueTodayOrOverdue) NegativeRed else WarningAmber
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // EMI Progress bar (Deep Navy)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${loan.paidEmis} of ${loan.totalEmis} EMIs paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${loan.remainingEmis} EMIs remaining", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium))
                        }

                        if (!loan.isClosed) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { selectedLoanForPayment = loan },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mark EMI as Paid (${CurrencyFormatter.format(loan.emiAmount)})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
        var dueDayText by remember { mutableStateOf("10") }
        var enableReminder by remember { mutableStateOf(true) }
        var notes by remember { mutableStateOf("") }

        var loanNameError by remember { mutableStateOf<String?>(null) }
        var originalAmountError by remember { mutableStateOf<String?>(null) }
        var remainingAmountError by remember { mutableStateOf<String?>(null) }
        var emiAmountError by remember { mutableStateOf<String?>(null) }
        var totalEmisError by remember { mutableStateOf<String?>(null) }
        var interestRateError by remember { mutableStateOf<String?>(null) }
        var dueDayError by remember { mutableStateOf<String?>(null) }

        val parsedDueDay = (dueDayText.toIntOrNull() ?: 10).coerceIn(1, 31)

        // Calculate upcoming 3 monthly EMI schedule
        val (firstDueDateMillis, secondDueDateMillis, thirdDueDateMillis) = remember(parsedDueDay) {
            val now = Calendar.getInstance()
            val today = now.get(Calendar.DAY_OF_MONTH)

            val cal1 = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (today <= parsedDueDay) {
                    val maxThisMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, parsedDueDay.coerceAtMost(maxThisMonth))
                } else {
                    add(Calendar.MONTH, 1)
                    val maxNextMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
                    set(Calendar.DAY_OF_MONTH, parsedDueDay.coerceAtMost(maxNextMonth))
                }
            }

            val cal2 = (cal1.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                val maxMonth2 = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, parsedDueDay.coerceAtMost(maxMonth2))
            }

            val cal3 = (cal2.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
                val maxMonth3 = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, parsedDueDay.coerceAtMost(maxMonth3))
            }

            Triple(cal1.timeInMillis, cal2.timeInMillis, cal3.timeInMillis)
        }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = {
                Text(
                    text = "Add Loan",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Loan Name
                    OutlinedTextField(
                        value = loanName,
                        onValueChange = {
                            loanName = it
                            if (it.isNotBlank()) loanNameError = null
                        },
                        label = { Text("Loan Name *") },
                        placeholder = { Text("e.g. Home Loan, Car Loan") },
                        isError = loanNameError != null,
                        supportingText = loanNameError?.let { { Text(it, color = NegativeRed) } },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Lender / Bank
                    OutlinedTextField(
                        value = lender,
                        onValueChange = { lender = it },
                        label = { Text("Lender / Bank") },
                        placeholder = { Text("e.g. SBI, HDFC, ICICI") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3 & 4. Original & Remaining Loan Amount
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = originalAmountText,
                            onValueChange = {
                                originalAmountText = it
                                if (remainingAmountText.isEmpty()) remainingAmountText = it
                                if ((it.toDoubleOrNull() ?: 0.0) > 0) originalAmountError = null
                            },
                            label = { Text("Original (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = originalAmountError != null,
                            supportingText = originalAmountError?.let { { Text(it, color = NegativeRed) } },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = remainingAmountText,
                            onValueChange = {
                                remainingAmountText = it
                                if ((it.toDoubleOrNull() ?: 0.0) >= 0) remainingAmountError = null
                            },
                            label = { Text("Remaining (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = remainingAmountError != null,
                            supportingText = remainingAmountError?.let { { Text(it, color = NegativeRed) } },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 5 & 6. EMI Amount & Total EMIs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emiAmountText,
                            onValueChange = {
                                emiAmountText = it
                                if ((it.toDoubleOrNull() ?: 0.0) > 0) emiAmountError = null
                            },
                            label = { Text("EMI Amount (₹) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = emiAmountError != null,
                            supportingText = emiAmountError?.let { { Text(it, color = NegativeRed) } },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = totalEmisText,
                            onValueChange = {
                                totalEmisText = it
                                if ((it.toIntOrNull() ?: 0) > 0) totalEmisError = null
                            },
                            label = { Text("Total EMIs *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = totalEmisError != null,
                            supportingText = totalEmisError?.let { { Text(it, color = NegativeRed) } },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 7. Interest Rate
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = {
                            interestRateText = it
                            if ((it.toDoubleOrNull() ?: 0.0) >= 0) interestRateError = null
                        },
                        label = { Text("Interest Rate (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = interestRateError != null,
                        supportingText = interestRateError?.let { { Text(it, color = NegativeRed) } },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 8. EMI Due Date (Day of Month)
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = {
                            dueDayText = it
                            val d = it.toIntOrNull()
                            if (d != null && d in 1..31) {
                                dueDayError = null
                            } else if (it.isNotBlank()) {
                                dueDayError = "Enter day between 1 and 31"
                            }
                        },
                        label = { Text("EMI Due Date (Day of Month) *") },
                        placeholder = { Text("e.g. 10 for the 10th of every month") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = dueDayError != null,
                        supportingText = dueDayError?.let { { Text(it, color = NegativeRed) } },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ORYVO Automatic Monthly Schedule Calculation
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Monthly EMI Schedule",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ORYVO automatically calculates upcoming dues for day $parsedDueDay of each month:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Next: ${DateUtils.formatFullDate(firstDueDateMillis)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = "• Following: ${DateUtils.formatFullDate(secondDueDateMillis)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = "• Later: ${DateUtils.formatFullDate(thirdDueDateMillis)}",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    // 9. Single EMI Reminder Setting
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Monthly EMI Reminder",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (enableReminder) "Repeats automatically every month on day $parsedDueDay (9:00 AM)" else "No reminder scheduled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = enableReminder,
                                onCheckedChange = { enableReminder = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        var isValid = true
                        if (loanName.isBlank()) {
                            loanNameError = "Loan name is required"
                            isValid = false
                        } else {
                            loanNameError = null
                        }

                        val original = originalAmountText.toDoubleOrNull()
                        if (original == null || original <= 0.0) {
                            originalAmountError = "Enter amount > 0"
                            isValid = false
                        } else {
                            originalAmountError = null
                        }

                        val remaining = remainingAmountText.toDoubleOrNull()
                        if (remaining == null || remaining < 0.0) {
                            remainingAmountError = "Cannot be negative"
                            isValid = false
                        } else {
                            remainingAmountError = null
                        }

                        val emi = emiAmountText.toDoubleOrNull()
                        if (emi == null || emi <= 0.0) {
                            emiAmountError = "Enter EMI > 0"
                            isValid = false
                        } else {
                            emiAmountError = null
                        }

                        val totalEmis = totalEmisText.toIntOrNull()
                        if (totalEmis == null || totalEmis <= 0) {
                            totalEmisError = "Enter EMIs > 0"
                            isValid = false
                        } else {
                            totalEmisError = null
                        }

                        val interest = interestRateText.toDoubleOrNull()
                        if (interest == null || interest < 0.0) {
                            interestRateError = "Enter valid rate"
                            isValid = false
                        } else {
                            interestRateError = null
                        }

                        val dueDay = dueDayText.toIntOrNull()
                        if (dueDay == null || dueDay !in 1..31) {
                            dueDayError = "Enter day (1–31)"
                            isValid = false
                        } else {
                            dueDayError = null
                        }

                        if (isValid && original != null && remaining != null && emi != null && totalEmis != null && dueDay != null) {
                            viewModel.addLoan(
                                name = loanName.trim(),
                                lender = lender.trim().ifBlank { "Bank" },
                                originalAmount = original,
                                remainingAmount = remaining,
                                interestRate = interest ?: 0.0,
                                emiAmount = emi,
                                dueDayOfMonth = dueDay,
                                totalEmis = totalEmis,
                                notes = notes.trim(),
                                nextDueDateMillis = firstDueDateMillis,
                                enableReminder = enableReminder
                            )
                            showAddLoanDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Loan", fontWeight = FontWeight.SemiBold)
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
            title = { Text("Record EMI Payment", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Loan: ${loan.name} (${loan.lender})", style = MaterialTheme.typography.bodyMedium)
                    Text("EMI Amount: ${CurrencyFormatter.format(loan.emiAmount)}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text("This will deduct ${CurrencyFormatter.format(loan.emiAmount)} from selected account, reduce remaining loan balance, and advance the next monthly due date.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
                                Text("${acc.name} (${CurrencyFormatter.format(acc.currentBalance)})", style = MaterialTheme.typography.bodyMedium)
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
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
