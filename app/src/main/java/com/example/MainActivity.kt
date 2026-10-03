package com.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.example.ui.components.QuickAddSheet
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.FinanceViewModelFactory

enum class MoreSubScreen {
    NONE,
    ACCOUNTS,
    REMINDERS,
    CATEGORIES,
    SMS_DETECTION,
    NOTIFICATION_DETECTION,
    PERMISSIONS_DETECTION,
    ANALYTICS,
    SECURITY,
    BACKUP_EXPORT,
    PARSER_DEBUG
}

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: FinanceViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as FinFlowApplication
        val factory = FinanceViewModelFactory(app, app.repository)
        viewModel = ViewModelProvider(this, factory)[FinanceViewModel::class.java]

        val initialScreenExtra = intent.getStringExtra("OPEN_SCREEN")

        setContent {
            MyApplicationTheme {
                MainAppContent(
                    viewModel = viewModel,
                    initialScreen = initialScreenExtra
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.lockApp()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: FinanceViewModel,
    initialScreen: String?
) {
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var financeInitialTab by remember { mutableStateOf(0) }
    var moreSubScreen by remember { mutableStateOf(MoreSubScreen.NONE) }
    var showQuickAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialScreen) {
        when (initialScreen) {
            "dashboard", "home" -> {
                selectedTab = 0
                moreSubScreen = MoreSubScreen.NONE
            }
            "transactions" -> {
                selectedTab = 1
                moreSubScreen = MoreSubScreen.NONE
            }
            "savings" -> {
                selectedTab = 3
                financeInitialTab = 0
                moreSubScreen = MoreSubScreen.NONE
            }
            "investments" -> {
                selectedTab = 3
                financeInitialTab = 1
                moreSubScreen = MoreSubScreen.NONE
            }
            "loans", "emi" -> {
                selectedTab = 3
                financeInitialTab = 2
                moreSubScreen = MoreSubScreen.NONE
            }
            "finance" -> {
                selectedTab = 3
                moreSubScreen = MoreSubScreen.NONE
            }
            "accounts" -> {
                selectedTab = 4
                moreSubScreen = MoreSubScreen.ACCOUNTS
            }
            "sms_detection" -> {
                selectedTab = 4
                moreSubScreen = MoreSubScreen.SMS_DETECTION
            }
            "reminders" -> {
                selectedTab = 4
                moreSubScreen = MoreSubScreen.REMINDERS
            }
            "permissions" -> {
                selectedTab = 4
                moreSubScreen = MoreSubScreen.PERMISSIONS_DETECTION
            }
            "security" -> {
                selectedTab = 4
                moreSubScreen = MoreSubScreen.SECURITY
            }
        }
    }

    // Handle back button for sub screens
    if (moreSubScreen != MoreSubScreen.NONE) {
        BackHandler {
            moreSubScreen = MoreSubScreen.NONE
        }
    } else if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    // 0. Minimal ORYVO Splash Screen
    var isInitialSplashVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(650)
        isInitialSplashVisible = false
    }

    if (isInitialSplashVisible) {
        com.example.ui.components.OryvoSplashScreen()
        return
    }

    // 1. App Lock Screen
    if (isAppLocked) {
        LockScreen(viewModel = viewModel)
        return
    }

    // 2. Onboarding Screen on First Launch
    if (appSettings != null && !appSettings!!.hasCompletedOnboarding) {
        OnboardingScreen(
            viewModel = viewModel,
            onFinish = {
                // Completed onboarding
            }
        )
        return
    }

    // 3. Main Scaffold with Bottom Navigation
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (moreSubScreen == MoreSubScreen.NONE) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home", fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                contentDescription = "Transactions"
                            )
                        },
                        label = { Text("Transactions", fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_transactions")
                    )

                    // Center Quick Add Button
                    NavigationBarItem(
                        selected = false,
                        onClick = { showQuickAddSheet = true },
                        icon = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Transaction",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        },
                        label = { Text("Add", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_add")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                                contentDescription = "Finance"
                            )
                        },
                        label = { Text("Finance", fontWeight = if (selectedTab == 3) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_finance")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 4) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontWeight = if (selectedTab == 4) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = navItemColors,
                        modifier = Modifier.testTag("nav_more")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { selectedTab = 1 },
                    onNavigateToAccounts = {
                        selectedTab = 4
                        moreSubScreen = MoreSubScreen.ACCOUNTS
                    },
                    onNavigateToSmsReview = {
                        selectedTab = 4
                        moreSubScreen = MoreSubScreen.SMS_DETECTION
                    },
                    onNavigateToReminders = {
                        selectedTab = 4
                        moreSubScreen = MoreSubScreen.REMINDERS
                    },
                    onOpenQuickAdd = { showQuickAddSheet = true }
                )
                1 -> TransactionsScreen(viewModel = viewModel)
                3 -> FinanceScreen(viewModel = viewModel, initialTab = financeInitialTab)
                4 -> {
                    when (moreSubScreen) {
                        MoreSubScreen.NONE -> MoreScreen(
                            viewModel = viewModel,
                            onNavigateToAccounts = { moreSubScreen = MoreSubScreen.ACCOUNTS },
                            onNavigateToReminders = { moreSubScreen = MoreSubScreen.REMINDERS },
                            onNavigateToCategories = { moreSubScreen = MoreSubScreen.CATEGORIES },
                            onNavigateToPermissionsDetection = { moreSubScreen = MoreSubScreen.PERMISSIONS_DETECTION },
                            onNavigateToSmsDetection = { moreSubScreen = MoreSubScreen.SMS_DETECTION },
                            onNavigateToNotificationDetection = { moreSubScreen = MoreSubScreen.NOTIFICATION_DETECTION },
                            onNavigateToAnalytics = { moreSubScreen = MoreSubScreen.ANALYTICS },
                            onNavigateToSecurity = { moreSubScreen = MoreSubScreen.SECURITY },
                            onNavigateToBackupExport = { moreSubScreen = MoreSubScreen.BACKUP_EXPORT },
                            onNavigateToParserDebug = { moreSubScreen = MoreSubScreen.PARSER_DEBUG }
                        )
                        MoreSubScreen.PERMISSIONS_DETECTION -> PermissionsDetectionScreen(
                            viewModel = viewModel,
                            onNavigateBack = { moreSubScreen = MoreSubScreen.NONE },
                            onNavigateToDebugger = { moreSubScreen = MoreSubScreen.PARSER_DEBUG }
                        )
                        MoreSubScreen.ACCOUNTS -> AccountsScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.REMINDERS -> RemindersScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.CATEGORIES -> CategoriesScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.SMS_DETECTION -> SmsDetectionScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.NOTIFICATION_DETECTION -> NotificationDetectionScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.ANALYTICS -> AnalyticsScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.SECURITY -> SecurityScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.BACKUP_EXPORT -> BackupExportScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                        MoreSubScreen.PARSER_DEBUG -> ParserDebugScreen(
                            viewModel = viewModel,
                            onBack = { moreSubScreen = MoreSubScreen.NONE }
                        )
                    }
                }
                else -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { selectedTab = 1 },
                    onNavigateToAccounts = {
                        selectedTab = 4
                        moreSubScreen = MoreSubScreen.ACCOUNTS
                    },
                    onNavigateToSmsReview = {
                        selectedTab = 4
                        moreSubScreen = MoreSubScreen.SMS_DETECTION
                    },
                    onNavigateToReminders = {
                        selectedTab = 4
                        moreSubScreen = MoreSubScreen.REMINDERS
                    },
                    onOpenQuickAdd = { showQuickAddSheet = true }
                )
            }
        }
    }

    // Quick Add Bottom Sheet
    if (showQuickAddSheet) {
        QuickAddSheet(
            viewModel = viewModel,
            onDismiss = { showQuickAddSheet = false }
        )
    }
}
