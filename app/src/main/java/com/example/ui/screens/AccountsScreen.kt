package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Account
import com.example.data.entity.AccountType
import com.example.ui.components.AccountCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val accounts by viewModel.accounts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingAccountHistory by remember { mutableStateOf<Account?>(null) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts & Balances", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Account")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("accounts_screen"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Security disclaimer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Privacy Shield: FinFlow never requests or stores internet banking passwords, ATM PINs, CVV, or full account numbers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No accounts found", style = MaterialTheme.typography.titleMedium)
                            Text("Tap '+' to add your first Bank or Cash account", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(accounts, key = { it.id }) { acc ->
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
                                        Text(acc.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        if (acc.isArchived) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                                Text("Archived", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    val subtitle = if (acc.accountNumberLast4.isNotBlank()) "${acc.bankName} (****${acc.accountNumberLast4})" else acc.bankName
                                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row {
                                    IconButton(onClick = { editingAccount = acc }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { viewModel.deleteAccount(acc) }) {
                                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ExpenseRed)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("Opening: ${CurrencyFormatter.format(acc.openingBalance)}", style = MaterialTheme.typography.labelSmall)
                                    Text("Current Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(CurrencyFormatter.format(acc.currentBalance), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
                                }

                                OutlinedButton(
                                    onClick = { viewingAccountHistory = acc },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("History", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var bankName by remember { mutableStateOf("") }
        var last4 by remember { mutableStateOf("") }
        var openingBalanceText by remember { mutableStateOf("") }
        var accountType by remember { mutableStateOf(AccountType.BANK) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Nickname") },
                        placeholder = { Text("e.g. SBI Savings, Personal Cash") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank / Institution Name") },
                        placeholder = { Text("e.g. State Bank of India, HDFC") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = last4,
                        onValueChange = { if (it.length <= 4) last4 = it },
                        label = { Text("Last 4 digits (Optional)") },
                        placeholder = { Text("1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = openingBalanceText,
                        onValueChange = { openingBalanceText = it },
                        label = { Text("Opening Balance (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Account Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AccountType.values().take(3).forEach { t ->
                            FilterChip(
                                selected = accountType == t,
                                onClick = { accountType = t },
                                label = { Text(t.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val openingBal = openingBalanceText.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            viewModel.addAccount(
                                name = name.trim(),
                                bankName = bankName.trim().ifBlank { "Cash / Other" },
                                accountNumberLast4 = last4.trim(),
                                openingBalance = openingBal,
                                accountType = accountType
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Edit Account Dialog
    editingAccount?.let { acc ->
        var name by remember { mutableStateOf(acc.name) }
        var bankName by remember { mutableStateOf(acc.bankName) }
        var isArchived by remember { mutableStateOf(acc.isArchived) }

        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text("Edit Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = isArchived, onCheckedChange = { isArchived = it })
                        Text("Archive this account")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.updateAccount(acc.copy(name = name.trim(), bankName = bankName.trim(), isArchived = isArchived))
                            editingAccount = null
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) { Text("Cancel") }
            }
        )
    }

    // Account Transaction History Dialog
    viewingAccountHistory?.let { acc ->
        val transactions by viewModel.transactions.collectAsState()
        val accountTx = transactions.filter { it.accountId == acc.id || it.toAccountId == acc.id }

        AlertDialog(
            onDismissRequest = { viewingAccountHistory = null },
            title = {
                Column {
                    Text(acc.name, fontWeight = FontWeight.Bold)
                    Text(
                        "Balance: ${CurrencyFormatter.format(acc.currentBalance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                if (accountTx.isEmpty()) {
                    Text("No transactions found for this account.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(accountTx) { tx ->
                            TransactionItemCard(transaction = tx)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingAccountHistory = null }) {
                    Text("Done")
                }
            }
        )
    }
}
