package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: Double, showDecimals: Boolean = false, symbol: String = "₹"): String {
        val isNegative = amount < 0
        val absAmount = Math.abs(amount)

        // Custom Indian Rupee formatter: ##,##,##,###.##
        val integerPart = absAmount.toLong()
        val decimalPart = Math.round((absAmount - integerPart) * 100).toInt()

        val intStr = integerPart.toString()
        val formattedInt = if (intStr.length <= 3) {
            intStr
        } else {
            val lastThree = intStr.substring(intStr.length - 3)
            val rest = intStr.substring(0, intStr.length - 3)
            val chunked = StringBuilder()
            var count = 0
            for (i in rest.length - 1 downTo 0) {
                chunked.insert(0, rest[i])
                count++
                if (count == 2 && i != 0) {
                    chunked.insert(0, ',')
                    count = 0
                }
            }
            "$chunked,$lastThree"
        }

        val result = if (showDecimals && decimalPart > 0) {
            "$formattedInt.${String.format(Locale.US, "%02d", decimalPart)}"
        } else {
            formattedInt
        }

        return if (isNegative) "-$symbol$result" else "$symbol$result"
    }

    fun formatCompact(amount: Double, symbol: String = "₹"): String {
        val abs = Math.abs(amount)
        val isNeg = amount < 0
        val prefix = if (isNeg) "-$symbol" else symbol
        return when {
            abs >= 10_000_000 -> "$prefix${String.format(Locale.US, "%.2f", abs / 10_000_000)} Cr"
            abs >= 100_000 -> "$prefix${String.format(Locale.US, "%.2f", abs / 100_000)} L"
            abs >= 1_000 -> "$prefix${String.format(Locale.US, "%.1f", abs / 1_000)} k"
            else -> format(amount, false, symbol)
        }
    }
}
