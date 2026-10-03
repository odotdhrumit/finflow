package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.entity.ConfirmationStatus
import com.example.data.entity.DetectedMessage
import com.example.data.entity.TransactionType
import com.example.parser.SmsTransactionParser
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.LoanAmber
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils
import com.example.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsDetectionScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pendingDetections by viewModel.pendingDetections.collectAsState()
    val allDetections by viewModel.allDetections.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val accounts by viewModel.activeAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var testSmsText by remember {
        mutableStateOf("Your A/C XX1234 is debited by Rs.850.00 on 12-Oct-26 at Zomato. UPI Ref 38291039.")
    }
    var testResultBanner by remember { mutableStateOf<String?>(null) }
    var editingDetectedMessage by remember { mutableStateOf<DetectedMessage?>(null) }

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasSmsPermission = results[Manifest.permission.RECEIVE_SMS] == true
        if (hasSmsPermission) {
            viewModel.updateSmsDetection(true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bank SMS Detection", fontWeight = FontWeight.Bold) },
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
                .testTag("sms_detection_screen"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission & Enable Switch
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SMS Transaction Detection",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Listen for bank debit & credit SMS messages locally on device",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = appSettings?.enableSmsDetection == true,
                                onCheckedChange = { enable ->
                                    if (enable && !hasSmsPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.RECEIVE_SMS,
                                                Manifest.permission.READ_SMS
                                            )
                                        )
                                    } else {
                                        viewModel.updateSmsDetection(enable)
                                    }
                                }
                            )
                        }

                        if (!hasSmsPermission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant SMS Permission", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))

                        // Auto confirm toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Automatically confirm trusted bank transactions",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Off by default. When off, detected transactions are held for review.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = appSettings?.autoConfirmTrustedSms == true,
                                onCheckedChange = { viewModel.updateAutoConfirm(it) }
                            )
                        }
                    }
                }
            }

            // Pending Review Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Review Queue (${pendingDetections.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (pendingDetections.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("All caught up!", fontWeight = FontWeight.Bold)
                            Text(
                                "No pending bank messages to review. Test a message below to try the parser.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(pendingDetections, key = { it.id }) { msg ->
                    val isDebit = msg.parsedType != TransactionType.INCOME
                    val typeColor = if (isDebit) ExpenseRed else IncomeGreen
                    val typeLabel = if (isDebit) "Expense" else "Income"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header: TRANSACTION DETECTED
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = LoanAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "TRANSACTION DETECTED",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = LoanAmber),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = DateUtils.formatTime(msg.detectedAtMillis),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bank & Account
                            val bankAccountStr = "${msg.parsedBank ?: "Bank"} ${if (!msg.parsedAccountLast4.isNullOrEmpty()) "****${msg.parsedAccountLast4}" else ""}"
                            Text(
                                text = bankAccountStr,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Amount & Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = CurrencyFormatter.format(msg.parsedAmount ?: 0.0),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = typeColor)
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = typeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = typeColor),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Suggested category & merchant
                            Row {
                                Text("Suggested Category: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(msg.suggestedCategory ?: "Other", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                            if (!msg.parsedMerchant.isNullOrBlank()) {
                                Row {
                                    Text("Merchant: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(msg.parsedMerchant, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Raw SMS snippet
                            Text(
                                text = "\"${msg.rawText}\"",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons: [Confirm] [Edit] [Ignore]
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.ignoreDetected(msg) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Ignore", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { editingDetectedMessage = msg },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Edit", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val matchingAcc = accounts.find { it.accountNumberLast4 == msg.parsedAccountLast4 }
                                            ?: accounts.firstOrNull()

                                        if (matchingAcc != null) {
                                            viewModel.confirmDetected(
                                                message = msg,
                                                accountId = matchingAcc.id,
                                                category = msg.suggestedCategory ?: "Other",
                                                amount = msg.parsedAmount ?: 0.0,
                                                type = msg.parsedType ?: TransactionType.EXPENSE,
                                                merchant = msg.parsedMerchant ?: (msg.parsedBank ?: "Bank")
                                            )
                                        } else {
                                            // No accounts exist, open edit dialog to pick/add
                                            editingDetectedMessage = msg
                                        }
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                                ) {
                                    Text("Confirm", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Live Bank SMS Parser Test Workbench
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Live SMS Parser Test Workbench",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Paste or edit a real/sample Indian bank SMS message to test the parser instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = testSmsText,
                            onValueChange = { testSmsText = it },
                            label = { Text("SMS Message Text") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick sample buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testSmsText = "Your A/C XX1234 is debited by Rs.850.00 on 12-Oct-26 at Zomato. UPI Ref 38291039."
                                },
                                label = { Text("Debit/Zomato", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testSmsText = "Your a/c no. XX6789 is credited with INR 35,000.00 by salary from Tech Corp."
                                },
                                label = { Text("Credit/Salary", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    testSmsText = "Rs. 1,299.00 spent on your HDFC Card ending 4321 at Amazon India."
                                },
                                label = { Text("HDFC/Amazon", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val success = viewModel.testParseSms(testSmsText)
                                testResultBanner = if (success) {
                                    "Successfully parsed! Added to Review Queue or Auto-Confirmed."
                                } else {
                                    "Could not confidently identify this transaction. Checked against duplicate protection or insufficient financial data."
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Parse Message")
                        }

                        testResultBanner?.let { banner ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (banner.startsWith("Successfully")) IncomeGreen.copy(alpha = 0.15f) else LoanAmber.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = banner,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Detected Transaction Dialog
    editingDetectedMessage?.let { msg ->
        var editAmountText by remember { mutableStateOf((msg.parsedAmount ?: 0.0).toString()) }
        var editCategory by remember { mutableStateOf(msg.suggestedCategory ?: "Food") }
        var editMerchant by remember { mutableStateOf(msg.parsedMerchant ?: (msg.parsedBank ?: "Bank")) }
        var editType by remember { mutableStateOf(msg.parsedType ?: TransactionType.EXPENSE) }
        var selectedAccId by remember {
            mutableStateOf(
                accounts.find { it.accountNumberLast4 == msg.parsedAccountLast4 }?.id ?: accounts.firstOrNull()?.id
            )
        }

        AlertDialog(
            onDismissRequest = { editingDetectedMessage = null },
            title = { Text("Edit Detected Transaction", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editMerchant,
                        onValueChange = { editMerchant = it },
                        label = { Text("Merchant / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (accounts.isNotEmpty()) {
                        Text("Select Account:", style = MaterialTheme.typography.labelSmall)
                        accounts.forEach { acc ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RadioButton(
                                    selected = selectedAccId == acc.id,
                                    onClick = { selectedAccId = acc.id }
                                )
                                Text("${acc.name} (${acc.bankName})")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = editAmountText.toDoubleOrNull() ?: (msg.parsedAmount ?: 0.0)
                        val accId = selectedAccId ?: accounts.firstOrNull()?.id
                        if (accId != null && amt > 0) {
                            viewModel.confirmDetected(
                                message = msg,
                                accountId = accId,
                                category = editCategory.trim(),
                                amount = amt,
                                type = editType,
                                merchant = editMerchant.trim()
                            )
                            editingDetectedMessage = null
                        }
                    }
                ) {
                    Text("Confirm Transaction")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDetectedMessage = null }) { Text("Cancel") }
            }
        )
    }
}
