package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        TransactionType.INCOME -> Pair(PositiveGreen, "+")
        TransactionType.EXPENSE -> Pair(NegativeRed, "-")
        TransactionType.TRANSFER -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "")
        TransactionType.INVESTMENT -> Pair(NegativeRed, "-")
        TransactionType.LOAN_PAYMENT -> Pair(NegativeRed, "-")
        TransactionType.SAVINGS -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, "")
    }

    val icon = getCategoryIcon(transaction.categoryName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Subtle circular icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.categoryName,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
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
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "SMS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (transaction.source == TransactionSource.NOTIFICATION) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "APP",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        fontWeight = FontWeight.SemiBold,
                        color = typeColor
                    )
                )

                Text(
                    text = when (transaction.type) {
                        TransactionType.INCOME -> "Credit"
                        TransactionType.EXPENSE -> "Debit"
                        TransactionType.TRANSFER -> "Transfer"
                        TransactionType.INVESTMENT -> "Investment"
                        TransactionType.LOAN_PAYMENT -> "Loan EMI"
                        TransactionType.SAVINGS -> "Savings"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getCategoryIcon(categoryName: String): ImageVector {
    val lower = categoryName.lowercase()
    return when {
        lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("swiggy") || lower.contains("zomato") -> Icons.Default.Restaurant
        lower.contains("grocery") || lower.contains("supermarket") || lower.contains("blinkit") || lower.contains("zepto") -> Icons.Default.LocalGroceryStore
        lower.contains("shopping") || lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") -> Icons.Default.ShoppingBag
        lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") || lower.contains("cng") -> Icons.Default.LocalGasStation
        lower.contains("transport") || lower.contains("uber") || lower.contains("ola") || lower.contains("metro") || lower.contains("train") || lower.contains("travel") -> Icons.Default.DirectionsTransit
        lower.contains("bill") || lower.contains("electricity") || lower.contains("water") || lower.contains("broadband") || lower.contains("recharge") -> Icons.Default.Receipt
        lower.contains("entertainment") || lower.contains("movie") || lower.contains("netflix") || lower.contains("hotstar") || lower.contains("spotify") -> Icons.Default.Movie
        lower.contains("health") || lower.contains("medical") || lower.contains("pharmacy") || lower.contains("hospital") -> Icons.Default.LocalHospital
        lower.contains("salary") || lower.contains("income") || lower.contains("payroll") -> Icons.Default.Payments
        lower.contains("investment") || lower.contains("stock") || lower.contains("mutual") || lower.contains("sip") -> Icons.Default.TrendingUp
        lower.contains("loan") || lower.contains("emi") -> Icons.Default.CreditCard
        lower.contains("savings") -> Icons.Default.Savings
        lower.contains("upi") || lower.contains("transfer") -> Icons.Default.SwapHoriz
        else -> Icons.Default.AttachMoney
    }
}
