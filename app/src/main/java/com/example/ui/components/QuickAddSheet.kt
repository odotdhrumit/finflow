package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.entity.Account
import com.example.data.entity.Category
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
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
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
                            selectedContainerColor = when (type) {
                                TransactionType.EXPENSE -> ExpenseRed
                                TransactionType.INCOME -> IncomeGreen
                                TransactionType.TRANSFER -> TransferPurple
                                TransactionType.INVESTMENT -> InvestmentIndigo
                                TransactionType.LOAN_PAYMENT -> LoanAmber
                                TransactionType.SAVINGS -> SavingsTeal
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input with large Rupee symbol
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "₹",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            amountText = input
                        }
                    },
                    placeholder = { Text("0.00", fontSize = 28.sp) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("transaction_amount_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Selector
            if (accounts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Please add at least one account from More > Accounts first.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Text(
                    text = if (selectedType == TransactionType.TRANSFER) "From Account" else "Account",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        val isSelected = selectedAccountId == acc.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text("${acc.name} (${acc.bankName})") }
                        )
                    }
                }

                // If Transfer, show To Account
                if (selectedType == TransactionType.TRANSFER) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "To Account",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            val isSelected = selectedToAccountId == acc.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedToAccountId = acc.id },
                                label = { Text("${acc.name} (${acc.bankName})") }
                            )
                        }
                    }
                }
            }

            // Category Selector (for non-transfer)
            if (selectedType != TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                val filteredCategories = categories.filter {
                    if (selectedType == TransactionType.INCOME) it.isIncome else !it.isIncome
                }
                val catsToShow = if (filteredCategories.isNotEmpty()) filteredCategories else categories

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    catsToShow.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.name },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = getCategoryIcon(cat.name),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Merchant / Description
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = { Text(if (selectedType == TransactionType.INCOME) "Payer / Source" else "Merchant / Payee") },
                placeholder = { Text("e.g. Swiggy, Amazon, Salary") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_merchant_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
