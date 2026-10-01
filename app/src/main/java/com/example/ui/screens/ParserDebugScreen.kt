package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DetectedSourceType
import com.example.parser.*
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LoanAmber
import com.example.util.CurrencyFormatter
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParserDebugScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    var rawInputText by remember {
        mutableStateOf("Rs 100 credited to your account. Available balance Rs 8,500.")
    }
    var senderInput by remember { mutableStateOf("SBI") }
    var parsedResult by remember { mutableStateOf<ParsedTransaction?>(null) }
    var simulationMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Analyze initial text
    LaunchedEffect(Unit) {
        parsedResult = SmsTransactionParser.parse(senderInput, rawInputText)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Parser Debugger", fontWeight = FontWeight.Bold) },
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
                .testTag("parser_debug_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Test Financial Message",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Enter any bank SMS, UPI notification, or alert to test the universal semantic parser.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = senderInput,
                            onValueChange = { senderInput = it },
                            label = { Text("Sender Header / App Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = { rawInputText = it },
                            label = { Text("Raw Message Body") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    parsedResult = SmsTransactionParser.parse(senderInput, rawInputText)
                                    simulationMessage = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analyze")
                            }

                            FilledTonalButton(
                                onClick = {
                                    val currentParsed = parsedResult ?: SmsTransactionParser.parse(senderInput, rawInputText)
                                    if (currentParsed != null) {
                                        scope.launch {
                                            val processed = viewModel.testParseSms(currentParsed.rawText)
                                            simulationMessage = if (processed) "Processed successfully in repository engine." else "Rejected or duplicate."
                                        }
                                    }
                                }
                            ) {
                                Text("Simulate Pipeline")
                            }
                        }

                        simulationMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = IncomeGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Sample Presets:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SuggestionChip(
                                onClick = {
                                    rawInputText = "₹100 credited to your account. Available balance ₹5,850."
                                    senderInput = "SBI"
                                    parsedResult = SmsTransactionParser.parse(senderInput, rawInputText)
                                },
                                label = { Text("₹100 + ₹5,850 Bal", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    rawInputText = "Rs 4,000 debited. Available balance Rs 12,500."
                                    senderInput = "HDFC"
                                    parsedResult = SmsTransactionParser.parse(senderInput, rawInputText)
                                },
                                label = { Text("Rs 4k + 12.5k Bal", fontSize = 11.sp) }
                            )
                            SuggestionChip(
                                onClick = {
                                    rawInputText = "₹4,000 paid to Swiggy using UPI Ref 123456789012. Bal: ₹12,000"
                                    senderInput = "Google Pay"
                                    parsedResult = SmsTransactionParser.parse(senderInput, rawInputText)
                                },
                                label = { Text("UPI Debit", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            // Results Section
            if (parsedResult != null) {
                val parsed = parsedResult!!

                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PARSED SEMANTIC ENTITIES",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                val confidenceColor = when (parsed.confidence) {
                                    ParseConfidence.HIGH -> IncomeGreen
                                    ParseConfidence.MEDIUM -> LoanAmber
                                    ParseConfidence.LOW -> ExpenseRed
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = confidenceColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "${parsed.confidence.name} CONFIDENCE",
                                        color = confidenceColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Entity Rows
                            DebugEntityRow("Selected Txn Amount", CurrencyFormatter.format(parsed.amount), isBold = true)
                            DebugEntityRow("Detected Direction", parsed.detectedType.name, valueColor = if (parsed.isDebit) ExpenseRed else IncomeGreen)
                            DebugEntityRow("Detected Balance", parsed.balanceAfterTransaction?.let { CurrencyFormatter.format(it) } ?: "Not Present")
                            DebugEntityRow("Detected Fee", parsed.feeAmount?.let { CurrencyFormatter.format(it) } ?: "None")
                            DebugEntityRow("Detected Bank/App", parsed.bank ?: "Unknown")
                            DebugEntityRow("Account Masked Last4", parsed.accountLast4 ?: "Not specified")
                            DebugEntityRow("Reference / UTR ID", parsed.referenceNumber.ifBlank { "None" })
                            DebugEntityRow("Merchant / Party", parsed.merchant.ifBlank { "None" })
                            DebugEntityRow("UPI VPA", parsed.upiId ?: "None")
                            DebugEntityRow("Suggested Category", parsed.suggestedCategory)
                            DebugEntityRow("Requires Review", if (parsed.requiresReview) "YES (Held for review)" else "NO (Eligible for auto-save)")
                            DebugEntityRow("Reasoning", parsed.confidenceReason.ifBlank { "Standard extraction" })

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Fingerprint Hash:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = parsed.fingerprint,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // All Detected Money Values Breakdown
                item {
                    Text(
                        text = "All Detected Monetary Values (${parsed.allDetectedMoney.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(parsed.allDetectedMoney) { candidate ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "₹${candidate.amount}  (raw: \"${candidate.rawValue}\")",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = candidate.reasoning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val roleColor = when (candidate.role) {
                                MoneyRole.TRANSACTION -> IncomeGreen
                                MoneyRole.BALANCE -> MaterialTheme.colorScheme.primary
                                MoneyRole.FEE -> ExpenseRed
                                MoneyRole.LIMIT -> LoanAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = roleColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = candidate.role.name,
                                    color = roleColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Text("Could not parse message or filtered as non-financial/OTP.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DebugEntityRow(label: String, value: String, isBold: Boolean = false, valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium),
            color = valueColor
        )
    }
}
