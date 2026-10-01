package com.example.parser

import java.math.BigDecimal
import java.math.RoundingMode
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
        Pair("PhonePe", listOf("PHONEPE", "PHNPE")),
        Pair("Cred", listOf("CRED")),
        Pair("BHIM", listOf("BHIM", "NPCI"))
    )

    // Regex for matching amount in Indian Rupees without premature truncation:
    // IMPORTANT: Note the greedy [0-9]{1,3}(?:,[0-9]{2,3})+ requiring at least one comma group
    // when commas are present, OR [0-9]+ to greedily take all digits when no commas exist.
    // The negative lookahead (?![0-9]) prevents truncating digits (e.g. 4000 will NOT be captured as 400).
    private val AMOUNT_PATTERNS = listOf(
        // 1. Currency prefix: ₹4,000, Rs. 4,000, INR 4,000, INR4000, ₹400, ₹4,00,000
        Pattern.compile(
            """(?:Rs\.?|INR|₹)\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?![0-9])""",
            Pattern.CASE_INSENSITIVE
        ),
        // 2. Currency suffix: 4,000 Rs, 4000 INR, 4,000.00 ₹
        Pattern.compile(
            """([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:Rs\.?|INR|₹)(?![0-9])""",
            Pattern.CASE_INSENSITIVE
        ),
        // 3. Banking action verb followed by amount (with or without currency indicator):
        // e.g. "debited by 4000", "credited with 4,000.00", "spent 400", "payment of 4,000"
        Pattern.compile(
            """(?:debited\s*(?:by|with|for)?|credited\s*(?:by|with|for|to)?|paid|spent|sent|received|withdrawn|deposited|transfer(?:red)?\s*(?:of|for)?|txn\s*(?:of)?|amount\s*(?:of)?|payment\s*(?:of)?)\s*(?:Rs\.?|INR|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?![0-9])""",
            Pattern.CASE_INSENSITIVE
        )
    )

    // Regex for matching account number / card ending: A/C XX1234, ending 1234, *1234, Acct 1234
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:a/c|acct|account|card|ac|c/c|d/c)(?:\s*(?:no\.?|num|ending|number))?[\s:*#xX-]*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:ending\s+with|ending\s+in|ending)\s*([0-9]{3,4})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""[xX*]{2,}([0-9]{3,4})""")
    )

    // Credit phrases
    private val CREDIT_PHRASES = listOf(
        "credited with", "credited to", "credited by", "credited",
        "money received", "upi received", "payment received", "received from", "received",
        "deposited", "added to", "transferred from", "refund", "cashback", "salary", "reversed"
    )

    // Debit phrases
    private val DEBIT_PHRASES = listOf(
        "debited by", "debited with", "debited",
        "upi payment", "payment made", "payment of", "paid to", "paid",
        "sent to", "sent", "spent at", "spent", "withdrawn from", "withdrawn",
        "transferred to", "purchase of", "purchase", "deducted", "charged", "withdrew", "txn of"
    )

    // Regex for reference / UPI RRN / UTR / Txn ID
    private val REF_PATTERNS = listOf(
        Pattern.compile("""(?:UPI\s*(?:ref|rrn|txn|id|reference)?|UTR|ref\s*(?:no\.?|id|num)?|txn\s*(?:id|no\.?)?)[:\s/]+([0-9a-zA-Z]{6,22})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UPI|IMPS|NEFT)/([0-9a-zA-Z]{6,22})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""RRN[:\s]*([0-9a-zA-Z]{6,22})""", Pattern.CASE_INSENSITIVE)
    )

    // Regex for merchant / payee / VPA
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:paid\s+to|transferred\s+to|sent\s+to|received\s+from|from|at|to|towards)\s+([A-Za-z0-9@._\-&]{3,30})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""VPA\s+([A-Za-z0-9@._\-]+)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""info[:\s]+([A-Za-z0-9@._\-&]{3,30})""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(sender: String, messageBody: String, timestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val lowerText = messageBody.lowercase(Locale.ENGLISH)

        // Strict OTP / Spam check: Do NOT parse login OTPs or verifications as transactions!
        if (isOtpOrVerification(lowerText)) {
            return null
        }

        // 1. Detect Bank / Source
        val detectedBank = detectBank(sender, messageBody)

        // 2. Extract Amount using safe decimal parser
        val amount = extractAmount(messageBody) ?: return null
        if (amount <= 0.0) return null

        // 3. Extract Debit or Credit
        val detectedType = detectDebitCredit(lowerText)

        // 4. Extract Account Last 4
        val accountLast4 = extractAccountLast4(messageBody)

        // 5. Extract Reference / UTR / UPI Ref number
        val refNo = extractRefNumber(messageBody)

        // 6. Extract Merchant / Description
        val merchant = extractMerchant(messageBody) ?: detectedBank

        // 7. Suggest Category
        val suggestedCategory = CategoryKeywordMatcher.suggestCategory(
            text = "$merchant $messageBody",
            isIncome = detectedType == DetectedTransactionType.CREDIT
        )

        // 8. Calculate Confidence Score
        var confidence = 0.5f
        if (accountLast4 != null) confidence += 0.2f
        if (refNo.isNotEmpty()) confidence += 0.15f
        if (detectedBank != "Bank") confidence += 0.15f
        if (detectedType == DetectedTransactionType.UNKNOWN) {
            confidence = 0.2f
        }

        val requiresReview = confidence < 0.7f ||
                detectedType == DetectedTransactionType.UNKNOWN ||
                accountLast4 == null

        // 9. Generate Stable Duplicate Fingerprint
        val fingerprint = generateFingerprint(
            bank = detectedBank,
            accountLast4 = accountLast4,
            amount = amount,
            type = detectedType,
            referenceNumber = refNo,
            merchant = merchant,
            timestamp = timestamp
        )

        return ParsedTransaction(
            bank = detectedBank,
            accountLast4 = accountLast4,
            detectedType = detectedType,
            amount = amount,
            merchant = merchant,
            referenceNumber = refNo,
            suggestedCategory = suggestedCategory,
            confidenceScore = confidence.coerceIn(0.0f, 1.0f),
            rawText = messageBody,
            timestamp = timestamp,
            fingerprint = fingerprint,
            requiresReview = requiresReview
        )
    }

    fun generateFingerprint(
        bank: String,
        accountLast4: String?,
        amount: Double,
        type: DetectedTransactionType,
        referenceNumber: String,
        merchant: String,
        timestamp: Long
    ): String {
        // Priority 1: Bank transaction/reference ID / UPI RRN / UTR
        if (referenceNumber.isNotBlank()) {
            val cleanRef = referenceNumber.trim().uppercase(Locale.ENGLISH)
            return sha256("REF-$cleanRef")
        }

        // Priority 2: Fallback fingerprint with 30-minute bucket + normalized fields
        val timeBucket = timestamp / (1000 * 60 * 30) // 30-minute window
        val amountFormatted = String.format(Locale.US, "%.2f", amount)
        val normMerchant = merchant.trim().lowercase(Locale.ENGLISH)
        val normAccount = accountLast4 ?: "NOACC"
        return sha256("FBN-$bank-$normAccount-$amountFormatted-$type-$normMerchant-$timeBucket")
    }

    private fun isOtpOrVerification(lowerText: String): Boolean {
        if (lowerText.contains("otp") || lowerText.contains("one time password") ||
            lowerText.contains("verification code") || lowerText.contains("security code") ||
            lowerText.contains("do not share this") || lowerText.contains("is your login code") ||
            lowerText.contains("is your secret code")
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

    fun extractAmount(text: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.replace(",", "")?.trim() ?: continue
                try {
                    val bd = BigDecimal(match).setScale(2, RoundingMode.HALF_UP)
                    if (bd > BigDecimal.ZERO) {
                        return bd.toDouble()
                    }
                } catch (e: Exception) {
                    // Fallthrough to next pattern if any
                }
            }
        }
        return null
    }

    fun detectDebitCredit(lowerText: String): DetectedTransactionType {
        // Find first occurrence of credit or debit phrases
        var firstCreditIndex = Int.MAX_VALUE
        for (phrase in CREDIT_PHRASES) {
            val idx = lowerText.indexOf(phrase)
            if (idx in 0 until firstCreditIndex) {
                firstCreditIndex = idx
            }
        }

        var firstDebitIndex = Int.MAX_VALUE
        for (phrase in DEBIT_PHRASES) {
            val idx = lowerText.indexOf(phrase)
            if (idx in 0 until firstDebitIndex) {
                firstDebitIndex = idx
            }
        }

        return when {
            firstCreditIndex != Int.MAX_VALUE && firstDebitIndex == Int.MAX_VALUE -> DetectedTransactionType.CREDIT
            firstDebitIndex != Int.MAX_VALUE && firstCreditIndex == Int.MAX_VALUE -> DetectedTransactionType.DEBIT
            firstCreditIndex != Int.MAX_VALUE && firstDebitIndex != Int.MAX_VALUE -> {
                if (firstCreditIndex < firstDebitIndex) DetectedTransactionType.CREDIT else DetectedTransactionType.DEBIT
            }
            lowerText.contains("transfer") -> DetectedTransactionType.TRANSFER
            else -> DetectedTransactionType.UNKNOWN
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

    fun extractRefNumber(text: String): String {
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
                if (!match.isNullOrEmpty() &&
                    !match.equals("rs", ignoreCase = true) &&
                    !match.equals("inr", ignoreCase = true) &&
                    !match.equals("upi", ignoreCase = true) &&
                    !match.equals("vpa", ignoreCase = true)
                ) {
                    // Clean up trailing punctuation
                    return match.trimEnd('.', ',', ':', ';', '-')
                }
            }
        }
        return null
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
