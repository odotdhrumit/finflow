package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.entity.Transaction
import com.example.data.entity.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.viewmodel.DateFilterType
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val filteredTransactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsState()
    val selectedDateFilter by viewModel.selectedDateFilter.collectAsState()

    var selectedTransactionForDetails by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen")
    ) {
        // Top Title
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search merchant, category, notes...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("transactions_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Date Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DateFilterType.values().forEach { filter ->
                val isSelected = selectedDateFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedDateFilter.value = filter },
                    label = {
                        Text(
                            when (filter) {
                                DateFilterType.ALL -> "All Time"
                                DateFilterType.TODAY -> "Today"
                                DateFilterType.THIS_WEEK -> "This Week"
                                DateFilterType.THIS_MONTH -> "This Month"
                            }
                        )
                    }
                )
            }
        }

        // Transaction Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == null,
                onClick = { viewModel.selectedTypeFilter.value = null },
                label = { Text("All Types") }
            )

            TransactionType.values().forEach { type ->
                val isSelected = selectedTypeFilter == type
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectedTypeFilter.value = if (isSelected) null else type },
                    label = {
                        Text(
                            when (type) {
                                TransactionType.INCOME -> "Income"
                                TransactionType.EXPENSE -> "Expense"
                                TransactionType.TRANSFER -> "Transfer"
                                TransactionType.INVESTMENT -> "Investment"
                                TransactionType.LOAN_PAYMENT -> "Loan"
                                TransactionType.SAVINGS -> "Savings"
                            }
                        )
                    }
                )
            }
        }

        // Transactions Count & Summary
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${filteredTransactions.size} transactions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val netFiltered = filteredTransactions.sumOf {
                when (it.type) {
                    TransactionType.INCOME -> it.amount
                    TransactionType.EXPENSE -> -it.amount
                    else -> 0.0
                }
            }
            Text(
                text = "Net: ${CurrencyFormatter.format(netFiltered)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (netFiltered >= 0) IncomeGreen else ExpenseRed
            )
        }

        // Transactions List
        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No matching transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try adjusting your search query or filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTransactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        onClick = { selectedTransactionForDetails = tx }
                    )
                }
            }
        }
    }

    // Transaction Details & Delete Dialog
    selectedTransactionForDetails?.let { tx ->
        AlertDialog(
            onDismissRequest = { selectedTransactionForDetails = null },
            title = {
                Text(
                    text = tx.merchant.ifBlank { tx.categoryName },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Amount:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(CurrencyFormatter.format(tx.amount), fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Type:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.type.name, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.categoryName, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Account:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.accountName, fontWeight = FontWeight.Medium)
                    }
                    if (tx.toAccountName != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("To Account:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(tx.toAccountName, fontWeight = FontWeight.Medium)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date & Time:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${DateUtils.formatDate(tx.dateMillis)} ${tx.timeFormatted}")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Source:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.source.name)
                    }
                    if (tx.notes.isNotBlank()) {
                        Divider()
                        Text("Notes:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tx.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTransactionForDetails = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        selectedTransactionForDetails = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                ) {
                    Text("Delete")
                }
            }
        )
    }
}
