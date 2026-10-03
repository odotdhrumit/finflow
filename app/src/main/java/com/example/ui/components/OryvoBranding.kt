package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.OryvoBackground
import com.example.ui.theme.OryvoPrimaryPurple
import com.example.ui.theme.OryvoTextSecondary

/**
 * Clean ORYVO vector logo symbol.
 * Completely abstract geometric financial flow & growth shape.
 */
@Composable
fun OryvoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_oryvo_logo),
        contentDescription = "ORYVO Logo",
        tint = Color.Unspecified,
        modifier = modifier.size(size)
    )
}

/**
 * Clean ORYVO Wordmark:
 * Modern geometric sans-serif, semi-bold, with clean letter-spacing.
 */
@Composable
fun OryvoWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = OryvoPrimaryPurple,
    letterSpacing: TextUnit = 2.5.sp
) {
    Text(
        text = "ORYVO",
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            color = color
        ),
        modifier = modifier
    )
}

/**
 * Combined ORYVO logo mark + wordmark for top bars and headers.
 */
@Composable
fun OryvoHeaderBranding(
    modifier: Modifier = Modifier,
    logoSize: Dp = 26.dp,
    textSize: TextUnit = 22.sp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OryvoLogo(size = logoSize)
        Spacer(modifier = Modifier.width(8.dp))
        OryvoWordmark(fontSize = textSize)
    }
}

/**
 * Minimal ORYVO splash screen:
 * [ORYVO SYMBOL]
 * ORYVO
 * Personal Finance Manager
 */
@Composable
fun OryvoSplashScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OryvoBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OryvoLogo(size = 80.dp)

            Spacer(modifier = Modifier.height(18.dp))

            OryvoWordmark(
                fontSize = 32.sp,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Personal Finance Manager",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.2.sp,
                    color = OryvoTextSecondary
                )
            )
        }
    }
}
