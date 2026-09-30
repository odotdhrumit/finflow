package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ConfirmationStatus
import com.example.data.entity.Transaction
import com.example.data.entity.TransactionSource
import com.example.data.entity.TransactionType
import com.example.ui.theme.*
import com.example.util.CurrencyFormatter
import com.example.util.DateUtils

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (typeColor, prefix) = when (transaction.type) {
        TransactionType.INCOME -> Pair(IncomeGreen, "+")
        TransactionType.EXPENSE -> Pair(ExpenseRed, "-")
        TransactionType.TRANSFER -> Pair(TransferPurple, "⇄ ")
        TransactionType.INVESTMENT -> Pair(InvestmentIndigo, "-")
        TransactionType.LOAN_PAYMENT -> Pair(LoanAmber, "-")
        TransactionType.SAVINGS -> Pair(SavingsTeal, "↓ ")
    }

    val icon = getCategoryIcon(transaction.categoryName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.categoryName,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Transaction Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.merchant.ifBlank { transaction.categoryName },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (transaction.source == TransactionSource.SMS) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "SMS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (transaction.source == TransactionSource.NOTIFICATION) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "APP",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val accountDesc = if (transaction.type == TransactionType.TRANSFER && transaction.toAccountName != null) {
                        "${transaction.accountName} → ${transaction.toAccountName}"
                    } else {
                        transaction.accountName.ifBlank { "Account" }
                    }

                    Text(
                        text = accountDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = DateUtils.formatDate(transaction.dateMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$prefix${CurrencyFormatter.format(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                )

                if (transaction.confirmationStatus == ConfirmationStatus.PENDING_REVIEW) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = LoanAmber
                    )
                }
            }
        }
    }
}

fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingBag
        "transport" -> Icons.Default.DirectionsCar
        "bills" -> Icons.Default.Receipt
        "home" -> Icons.Default.Home
        "education" -> Icons.Default.School
        "health" -> Icons.Default.LocalHospital
        "entertainment" -> Icons.Default.Movie
        "salary" -> Icons.Default.AttachMoney
        "investment" -> Icons.Default.TrendingUp
        "loan" -> Icons.Default.AccountBalance
        "savings" -> Icons.Default.Savings
        "cash withdrawal" -> Icons.Default.Atm
        "transfer" -> Icons.Default.SwapHoriz
        else -> Icons.Default.Category
    }
}
