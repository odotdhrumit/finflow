package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Account
import com.example.data.entity.AccountType
import com.example.util.CurrencyFormatter

@Composable
fun AccountCard(
    account: Account,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gradientColors = when (account.accountType) {
        AccountType.BANK -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
        AccountType.CASH -> listOf(Color(0xFFB45309), Color(0xFFF59E0B))
        AccountType.CREDIT_CARD -> listOf(Color(0xFF4C1D95), Color(0xFF7C3AED))
        AccountType.WALLET -> listOf(Color(0xFF065F46), Color(0xFF10B981))
        AccountType.OTHER -> listOf(Color(0xFF334155), Color(0xFF64748B))
    }

    val typeIcon = when (account.accountType) {
        AccountType.BANK -> Icons.Default.AccountBalance
        AccountType.CASH -> Icons.Default.Money
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.WALLET -> Icons.Default.Savings
        AccountType.OTHER -> Icons.Default.AccountBalance
    }

    Box(
        modifier = modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )
                    if (account.accountNumberLast4.isNotEmpty()) {
                        Text(
                            text = "****${account.accountNumberLast4}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.75f),
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = "Balance",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White.copy(alpha = 0.7f)
                    )
                )
                Text(
                    text = CurrencyFormatter.format(account.currentBalance),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
