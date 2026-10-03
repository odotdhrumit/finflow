package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TransactionType
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val accounts by viewModel.activeAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var selectedToAccountId by remember { mutableStateOf<Long?>(null) }
    var merchantText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    // Initialize default account
    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
        if (selectedToAccountId == null && accounts.size > 1) {
            selectedToAccountId = accounts[1].id
        }
    }

    // Update default category when type switches
    LaunchedEffect(selectedType) {
        when (selectedType) {
            TransactionType.INCOME -> selectedCategory = "Salary"
            TransactionType.EXPENSE -> selectedCategory = "Food"
            TransactionType.TRANSFER -> selectedCategory = "Transfer"
            TransactionType.INVESTMENT -> selectedCategory = "Investment"
            TransactionType.LOAN_PAYMENT -> selectedCategory = "Loan"
            TransactionType.SAVINGS -> selectedCategory = "Savings"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Transaction",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Type Chips (Horizontal Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.values().forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = {
                            Text(
                                text = when (type) {
                                    TransactionType.EXPENSE -> "Expense"
                                    TransactionType.INCOME -> "Income"
                                    TransactionType.TRANSFER -> "Transfer"
                                    TransactionType.INVESTMENT -> "Investment"
                                    TransactionType.LOAN_PAYMENT -> "Loan EMI"
                                    TransactionType.SAVINGS -> "Savings"
                                }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount (₹) *") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Merchant / Beneficiary
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = {
                    Text(
                        when (selectedType) {
                            TransactionType.INCOME -> "Received From / Employer"
                            TransactionType.TRANSFER -> "Transfer Note"
                            else -> "Merchant / Person"
                        }
                    )
                },
                placeholder = { Text("e.g. Amazon, Swiggy, Salary") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_merchant_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Account Selector
            if (accounts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (selectedType == TransactionType.TRANSFER) "From Account:" else "Account:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountId == acc.id,
                                onClick = { selectedAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Destination Account (for Transfer)
            if (selectedType == TransactionType.TRANSFER && accounts.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "To Account:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            FilterChip(
                                selected = selectedToAccountId == acc.id,
                                onClick = { selectedToAccountId = acc.id },
                                label = { Text(acc.name, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips (Horizontal Scrollable)
            if (selectedType != TransactionType.TRANSFER) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat.name,
                                onClick = { selectedCategory = cat.name },
                                label = { Text(cat.name, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Notes
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add any details or tags") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            val isValid = amountText.toDoubleOrNull() != null &&
                    amountText.toDouble() > 0 &&
                    selectedAccountId != null &&
                    (selectedType != TransactionType.TRANSFER || (selectedToAccountId != null && selectedToAccountId != selectedAccountId))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val accId = selectedAccountId ?: return@Button
                    viewModel.addTransaction(
                        amount = amount,
                        type = selectedType,
                        category = selectedCategory,
                        accountId = accId,
                        toAccountId = if (selectedType == TransactionType.TRANSFER) selectedToAccountId else null,
                        merchant = merchantText.trim(),
                        notes = noteText.trim()
                    )
                    onDismiss()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
