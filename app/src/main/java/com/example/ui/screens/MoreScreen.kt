package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.LoanAmber
import com.example.viewmodel.FinanceViewModel

@Composable
fun MoreScreen(
    viewModel: FinanceViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSmsDetection: () -> Unit,
    onNavigateToNotificationDetection: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToBackupExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingDetections by viewModel.pendingDetections.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDemoDataDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Account management, automation, analytics & settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Finance Management
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MoreMenuItem(
                        icon = Icons.Default.AccountBalance,
                        title = "Accounts",
                        subtitle = "Manage bank, cash & cards",
                        onClick = onNavigateToAccounts
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    MoreMenuItem(
                        icon = Icons.Default.Alarm,
                        title = "Payment Reminders",
                        subtitle = "Bill dues, loan EMIs & alerts",
                        onClick = onNavigateToReminders
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    MoreMenuItem(
                        icon = Icons.Default.Category,
                        title = "Categories",
                        subtitle = "Expense & income categories with smart keywords",
                        onClick = onNavigateToCategories
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    MoreMenuItem(
                        icon = Icons.Default.BarChart,
                        title = "Financial Analytics",
                        subtitle = "Spending trends & category breakdown",
                        onClick = onNavigateToAnalytics
                    )
                }
            }
        }

        // Section: Automation & Transaction Detection
        item {
            Text(
                text = "Automated Transaction Detection",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MoreMenuItem(
                        icon = Icons.Default.Sms,
                        title = "Bank SMS Detection",
                        subtitle = "Automatic parser & transaction review queue",
                        badge = if (pendingDetections.isNotEmpty()) "${pendingDetections.size} New" else null,
                        badgeColor = LoanAmber,
                        onClick = onNavigateToSmsDetection
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    MoreMenuItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Bank & App Notifications",
                        subtitle = "Detect UPI & bank notifications securely",
                        onClick = onNavigateToNotificationDetection
                    )
                }
            }
        }

        // Section: Privacy & Data
        item {
            Text(
                text = "Security & Storage",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MoreMenuItem(
                        icon = Icons.Default.Lock,
                        title = "App Lock & Security",
                        subtitle = "PIN and Biometric authentication",
                        onClick = onNavigateToSecurity
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    MoreMenuItem(
                        icon = Icons.Default.FileDownload,
                        title = "Backup & Export",
                        subtitle = "Export transactions to CSV spreadsheet",
                        onClick = onNavigateToBackupExport
                    )
                }
            }
        }

        // Section: Demo & Reset Actions
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MoreMenuItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Load Demo Data",
                        subtitle = "Seed sample SBI, HDFC, salary & transactions",
                        onClick = { showDemoDataDialog = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    MoreMenuItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear All Data",
                        subtitle = "Reset database and remove all transactions",
                        titleColor = ExpenseRed,
                        onClick = { showClearDataDialog = true }
                    )
                }
            }
        }

        // About card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FinFlow v1.0",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "100% Offline Personal Finance Manager. Your financial data, bank SMS, and balances never leave your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Demo Data Dialog
    if (showDemoDataDialog) {
        AlertDialog(
            onDismissRequest = { showDemoDataDialog = false },
            title = { Text("Load Sample Data?") },
            text = {
                Text("This will populate SBI and HDFC accounts, salary, food, shopping transactions, a new phone savings goal, and a personal loan for easy testing.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.seedDemoData()
                        showDemoDataDialog = false
                    }
                ) {
                    Text("Load Sample Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoDataDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?", color = ExpenseRed) },
            text = {
                Text("Are you sure you want to delete all accounts, transactions, savings, investments, and loan data? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    badge: String? = null,
    badgeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = titleColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = titleColor)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = badgeColor
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
