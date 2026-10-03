package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel

@Composable
fun MoreScreen(
    viewModel: FinanceViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToPermissionsDetection: () -> Unit,
    onNavigateToSmsDetection: () -> Unit,
    onNavigateToNotificationDetection: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToBackupExport: () -> Unit,
    onNavigateToParserDebug: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pendingDetections by viewModel.pendingDetections.collectAsState()
    var showClearDataDialog by remember { mutableStateOf(false) }
    var isDeveloperModeEnabled by remember { mutableStateOf(false) }
    var versionTapCount by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Preferences, automation & account management",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Section 1: FINANCE
        item {
            SectionHeader("FINANCE")
        }
        item {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.AccountBalance,
                    title = "Accounts",
                    subtitle = "Manage bank accounts, cash & cards",
                    onClick = onNavigateToAccounts
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Category,
                    title = "Categories",
                    subtitle = "Expense & income categories with smart keywords",
                    onClick = onNavigateToCategories
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.BarChart,
                    title = "Financial Analytics",
                    subtitle = "Spending trends & category breakdown",
                    onClick = onNavigateToAnalytics
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.FileDownload,
                    title = "Backup & Export",
                    subtitle = "Export transactions to CSV spreadsheet",
                    onClick = onNavigateToBackupExport
                )
            }
        }

        // Section 2: REMINDERS
        item {
            SectionHeader("REMINDERS")
        }
        item {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Alarm,
                    title = "Payment Reminders",
                    subtitle = "Bill dues, loan EMIs & payment alerts",
                    onClick = onNavigateToReminders
                )
            }
        }

        // Section 3: DETECTION
        item {
            SectionHeader("DETECTION & AUTOMATION")
        }
        item {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Tune,
                    title = "Permissions & Detection Setup",
                    subtitle = "SMS, notification access & background service",
                    onClick = onNavigateToPermissionsDetection
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Sms,
                    title = "Transaction Review Queue",
                    subtitle = "Review and confirm detected bank messages",
                    badge = if (pendingDetections.isNotEmpty()) "${pendingDetections.size} New" else null,
                    badgeColor = WarningAmber,
                    onClick = onNavigateToSmsDetection
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notification Detection Status",
                    subtitle = "Check UPI & bank notification listener status",
                    onClick = onNavigateToNotificationDetection
                )
            }
        }

        // Section 4: SECURITY
        item {
            SectionHeader("SECURITY")
        }
        item {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "App Lock & Security",
                    subtitle = "Biometrics fingerprint & security PIN",
                    onClick = onNavigateToSecurity
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.DeleteSweep,
                    title = "Clear All Data",
                    subtitle = "Reset database and remove all stored transactions",
                    titleColor = NegativeRed,
                    onClick = { showClearDataDialog = true }
                )
            }
        }

        // Developer Options (Completely hidden by default; unlocked only if developer mode is enabled)
        if (isDeveloperModeEnabled) {
            item {
                SectionHeader("DEVELOPER OPTIONS")
            }
            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Code,
                        title = "Developer Tools",
                        subtitle = "Universal parser debugger & synthetic message tests",
                        onClick = onNavigateToParserDebug
                    )
                }
            }
        }

        // About Card with ORYVO Logo
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(OryvoPrimaryPurple.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_oryvo_logo),
                            contentDescription = "ORYVO Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ORYVO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Personal Finance Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            versionTapCount++
                            if (versionTapCount >= 5) {
                                isDeveloperModeEnabled = !isDeveloperModeEnabled
                                versionTapCount = 0
                                Toast.makeText(
                                    context,
                                    if (isDeveloperModeEnabled) "Developer options enabled" else "Developer options disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(
                            text = "Version 1.0.0",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data?", color = NegativeRed, fontWeight = FontWeight.SemiBold) },
            text = {
                Text("Are you sure you want to delete all accounts, transactions, savings, investments, and loan data? This cannot be undone.", style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}

@Composable
private fun SettingsRow(
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
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (titleColor == NegativeRed) NegativeRed else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = titleColor)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}
