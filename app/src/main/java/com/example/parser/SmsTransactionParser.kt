package com.example.parser

import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

object SmsTransactionParser {

    private val BANK_HEADER_PATTERNS = listOf(
        Pair("SBI", listOf("SBI", "SBIN", "SBIPAY", "ATMSBI")),
        Pair("HDFC", listOf("HDFC", "HDFCBK")),
        Pair("ICICI", listOf("ICICI", "ICICIB")),
        Pair("Axis", listOf("AXIS", "AXISBK")),
        Pair("Kotak", listOf("KOTAK", "KMB")),
        Pair("PNB", listOf("PNBSMS", "PUNBNB")),
        Pair("Bank of Baroda", listOf("BOBSMS", "BOBTXN", "BARODA")),
        Pair("Canara", listOf("CANBNK", "CANARA")),
        Pair("IndusInd", listOf("INDBNK", "INDUS")),
        Pair("IDFC First", listOf("IDFC", "IDFCFB")),
        Pair("Yes Bank", listOf("YESBNK", "YESBANK")),
        Pair("Union Bank", listOf("UBIN", "UNIONB")),
        Pair("Paytm", listOf("PAYTM", "PYTM")),
        Pair("Google Pay", listOf("GPAY", "GOOGLEPAY")),
        Pair("PhonePe", listOf("PHONEPE", "PHNPE"))
    )

    // Regex for matching amount in Indian Rupees: Rs. 1,200.50, INR 500, ₹450, 850.00 INR
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:Rs\.?|INR|₹)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:Rs\.?|INR|₹)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:transaction of|txn of|amount of)\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // Regex for matching account number / card ending: A/C XX1234, ending 1234, *1234, Acct 1234
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:a/c|acct|account|card|ac|c/c|d/c)(?:\s*(?:no\.?|num|ending|number))?[\s:*#xX-]*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:ending\s+with|ending\s+in|ending)\s*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""[xX*]{2,}([0-9]{3,4})""")
    )

    // Regex for debit keywords
    private val DEBIT_KEYWORDS = listOf(
        "debited", "debit", "spent", "paid", "withdrawn", "sent",
        "purchase", "transferred to", "deducted", "used at", "txn of",
        "payment of", "charged", "withdrew"
    )

    // Regex for credit keywords
    private val CREDIT_KEYWORDS = listOf(
        "credited", "credit", "received", "deposited", "added to",
        "refund", "cashback", "salary", "transferred from", "reversed"
    )

    // Regex for reference / UPI RRN
    private val REF_PATTERNS = listOf(
        Pattern.compile("""(?:ref|rrn|upi ref|ref no|txn id|transaction id)[:\s]*([0-9a-zA-Z]{6,16})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""UPI/([0-9a-zA-Z]+)""", Pattern.CASE_INSENSITIVE)
    )

    // Regex for merchant / payee
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:at|to|info|towards|vpa|paid to|transferred to)\s+([A-Za-z0-9@._\-&]{3,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""VPA\s+([A-Za-z0-9@._\-]+)""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(sender: String, messageBody: String, timestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val lowerText = messageBody.lowercase(Locale.ENGLISH)

        // Strict OTP / Spam check: Do NOT parse login OTPs or verifications as transactions!
        if (isOtpOrVerification(lowerText)) {
            return null
        }

        // 1. Detect Bank
        val detectedBank = detectBank(sender, messageBody)

        // 2. Extract Amount
        val amount = extractAmount(messageBody) ?: return null
        if (amount <= 0.0) return null

        // 3. Extract Debit or Credit
        val isDebit = detectDebitCredit(lowerText) ?: return null

        // 4. Extract Account Last 4
        val accountLast4 = extractAccountLast4(messageBody)

        // 5. Extract Reference number
        val refNo = extractRefNumber(messageBody)

        // 6. Extract Merchant / Description
        val merchant = extractMerchant(messageBody) ?: detectedBank

        // 7. Suggest Category
        val suggestedCategory = CategoryKeywordMatcher.suggestCategory(
            text = "$merchant $messageBody",
            isIncome = !isDebit
        )

        // 8. Calculate Confidence Score
        var confidence = 0.5f
        if (accountLast4 != null) confidence += 0.2f
        if (refNo.isNotEmpty()) confidence += 0.15f
        if (detectedBank != "Bank") confidence += 0.15f

        // 9. Generate Duplicate Fingerprint
        // We use hash of Bank + AccountLast4 + Amount + (RefNo or rounded time)
        val timeKey = timestamp / (1000 * 60 * 30) // 30-minute bucket
        val fingerprintInput = "$detectedBank-$accountLast4-${String.format(Locale.US, "%.2f", amount)}-$isDebit-${if (refNo.isNotEmpty()) refNo else timeKey}"
        val fingerprint = sha256(fingerprintInput)

        return ParsedTransaction(
            bank = detectedBank,
            accountLast4 = accountLast4,
            isDebit = isDebit,
            amount = amount,
            merchant = merchant,
            referenceNumber = refNo,
            suggestedCategory = suggestedCategory,
            confidenceScore = confidence.coerceIn(0.0f, 1.0f),
            rawText = messageBody,
            timestamp = timestamp,
            fingerprint = fingerprint
        )
    }

    private fun isOtpOrVerification(lowerText: String): Boolean {
        if (lowerText.contains("otp") || lowerText.contains("one time password") ||
            lowerText.contains("verification code") || lowerText.contains("security code") ||
            lowerText.contains("do not share this") || lowerText.contains("is your login code")
        ) {
            // Check if it's strictly an authentication message rather than a transaction notification
            if (!lowerText.contains("debited") && !lowerText.contains("credited") && !lowerText.contains("spent")) {
                return true
            }
        }
        return false
    }

    private fun detectBank(sender: String, text: String): String {
        val upperSender = sender.uppercase(Locale.ENGLISH)
        for ((bank, keywords) in BANK_HEADER_PATTERNS) {
            for (kw in keywords) {
                if (upperSender.contains(kw) || text.contains(kw, ignoreCase = true)) {
                    return bank
                }
            }
        }
        return "Bank"
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.replace(",", "")?.trim()
                val parsed = match?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun detectDebitCredit(lowerText: String): Boolean? {
        val hasDebit = DEBIT_KEYWORDS.any { lowerText.contains(it) }
        val hasCredit = CREDIT_KEYWORDS.any { lowerText.contains(it) }

        return when {
            hasDebit && !hasCredit -> true
            hasCredit && !hasDebit -> false
            hasDebit && hasCredit -> {
                // Determine order of occurrence
                val firstDebitIndex = DEBIT_KEYWORDS.map { lowerText.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                val firstCreditIndex = CREDIT_KEYWORDS.map { lowerText.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: Int.MAX_VALUE
                firstDebitIndex < firstCreditIndex
            }
            else -> null // Unknown direction
        }
    }

    private fun extractAccountLast4(text: String): String? {
        for (pattern in ACCOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val group = matcher.group(1)
                if (!group.isNullOrEmpty() && group.length in 3..4) {
                    return group
                }
            }
        }
        return null
    }

    private fun extractRefNumber(text: String): String {
        for (pattern in REF_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.trim()
                if (!match.isNullOrEmpty()) return match
            }
        }
        return ""
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.trim()
                if (!match.isNullOrEmpty() && !match.equals("rs", ignoreCase = true) && !match.equals("inr", ignoreCase = true)) {
                    // Clean up punctuation
                    return match.trimEnd('.', ',', ':', ';')
                }
            }
        }
        return null
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
