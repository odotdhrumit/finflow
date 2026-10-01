package com.example.parser

import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

object SmsTransactionParser {

    // Bank / UPI App names
    private val KNOWN_FINANCIAL_ENTITIES = listOf(
        Pair("SBI", listOf("SBI", "SBIN", "SBIPAY", "ATMSBI", "YONO")),
        Pair("HDFC", listOf("HDFC", "HDFCBK")),
        Pair("ICICI", listOf("ICICI", "ICICIB", "IMOBILE")),
        Pair("Axis Bank", listOf("AXIS", "AXISBK")),
        Pair("Kotak Bank", listOf("KOTAK", "KMB", "811")),
        Pair("Punjab National Bank", listOf("PNBSMS", "PUNBNB", "PNB")),
        Pair("Bank of Baroda", listOf("BOBSMS", "BOBTXN", "BARODA")),
        Pair("Canara Bank", listOf("CANBNK", "CANARA")),
        Pair("IndusInd Bank", listOf("INDBNK", "INDUS")),
        Pair("IDFC First Bank", listOf("IDFC", "IDFCFB")),
        Pair("Yes Bank", listOf("YESBNK", "YESBANK")),
        Pair("Union Bank", listOf("UBIN", "UNIONB")),
        Pair("Paytm", listOf("PAYTM", "PYTM")),
        Pair("Google Pay", listOf("GPAY", "GOOGLEPAY", "GOOGLE PAY")),
        Pair("PhonePe", listOf("PHONEPE", "PHNPE")),
        Pair("CRED", listOf("CRED")),
        Pair("BHIM UPI", listOf("BHIM", "NPCI")),
        Pair("Amazon Pay", listOf("AMAZONPAY", "AMZN"))
    )

    // Regex to match any currency-like numeric string in Indian/Universal banking formats:
    // e.g. ₹100, ₹1,000, ₹4,000, ₹40,000, ₹4,00,000, ₹4,000.50, Rs. 4,000, INR 4000, 100.00
    private val NUMBER_TOKEN_PATTERN = Pattern.compile(
        """(?:\b|(?<=[₹RsINRs\.\s]))([0-9]{1,3}(?:,[0-9]{2,3})+(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?![0-9])""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for reference numbers, UTRs, RRNs, UPI IDs
    private val REF_PATTERNS = listOf(
        Pattern.compile("""(?:UPI\s*(?:ref|rrn|txn|id|reference)?|UTR|ref\s*(?:no\.?|id|num)?|txn\s*(?:id|no\.?)?|transaction\s*(?:id|no\.?)?|payment\s*id)[:\s/]+([0-9a-zA-Z]{6,26})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:UPI|IMPS|NEFT|RTGS)/([0-9a-zA-Z]{6,26})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\bRRN[:\s]*([0-9a-zA-Z]{6,26})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b(?:ref|rrn)\s*([0-9]{10,18})\b""", Pattern.CASE_INSENSITIVE)
    )

    // Regex for account identification
    private val ACCOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:a/c|acct|account|card|ac|c/c|d/c|savings\s*a/c)(?:\s*(?:no\.?|num|ending|number))?[\s:*#xX-]*([0-9]{3,4})\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:ending\s+with|ending\s+in|ending)\s*([0-9]{3,4})\b""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""[xX*]{2,}([0-9]{3,4})\b""")
    )

    // Regex for UPI VPA
    private val VPA_PATTERN = Pattern.compile(
        """\b([a-zA-Z0-9.\-_]{2,40}@[a-zA-Z0-9]{2,20})\b""",
        Pattern.CASE_INSENSITIVE
    )

    // Regex for merchant / payee / recipient / sender
    private val PERSON_OR_MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:paid\s+to|transferred\s+to|sent\s+to)\s+([A-Za-z0-9@._\-&\s]{2,28}?)(?=\s+(?:via|on|using|ref|avl|bal|upi|\.|\,|$))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:received\s+from|from)\s+([A-Za-z0-9@._\-&\s]{2,28}?)(?=\s+(?:via|on|using|ref|avl|bal|upi|\.|\,|$))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:at|towards)\s+([A-Za-z0-9@._\-&]{3,24})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""info[:\s]+([A-Za-z0-9@._\-&]{3,24})""", Pattern.CASE_INSENSITIVE)
    )

    // Balance context keywords
    private val BALANCE_KEYWORDS = listOf(
        "avl bal", "avail bal", "available bal", "available balance", "account balance",
        "current balance", "total bal", "ac bal", "a/c bal", "remaining bal", "remaining balance",
        "bal is", "bal:", "bal.", "bal ", "balance is", "balance:", "balance.", "balance ",
        "clear bal", "ledger bal", "new bal", "updated bal"
    )

    // Fee / Charge context keywords
    private val FEE_KEYWORDS = listOf(
        "fee", "charges", "charge", "convenience fee", "service fee", "tax", "gst", "surcharge"
    )

    // Limit context keywords
    private val LIMIT_KEYWORDS = listOf(
        "limit", "spending limit", "credit limit", "daily limit"
    )

    // Credit context keywords
    private val CREDIT_KEYWORDS = listOf(
        "credited with", "credited to", "credited by", "credited",
        "money received", "payment received", "upi received", "amount received",
        "funds received", "inward transaction", "inward", "received in", "received from", "received",
        "deposited in", "deposited", "added to", "added", "transferred from",
        "refund", "cashback", "salary", "reversed"
    )

    // Debit context keywords
    private val DEBIT_KEYWORDS = listOf(
        "debited by", "debited with", "debited",
        "paid to", "paid for", "paid",
        "spent at", "spent on", "spent",
        "payment made", "payment of", "withdrawn from", "withdrawn", "withdrew",
        "deducted from", "deducted", "sent to", "sent", "transferred to",
        "upi payment", "amount deducted", "outward transaction", "purchase of",
        "purchase at", "purchase", "charged", "txn of", "transaction of"
    )

    /**
     * Primary entry point: parses raw SMS / notification text semantically.
     */
    fun parse(sender: String, messageBody: String, timestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val rawText = messageBody.trim()
        if (rawText.isEmpty()) return null

        val lowerText = rawText.lowercase(Locale.ENGLISH)

        // Strict OTP / Non-Financial Filter
        if (isOtpOrNonFinancial(lowerText)) {
            return null
        }

        // 1. Text Normalization
        val normalizedText = normalizeMessage(rawText)

        // 2. Extract All Money Candidates & Semantic Classification
        val moneyCandidates = extractAllMoneyCandidates(rawText, normalizedText)

        // 3. Entity Extractions
        val detectedBank = detectBank(sender, rawText)
        val accountLast4 = extractAccountLast4(rawText)
        val refNumber = extractRefNumber(rawText)
        val upiId = extractUpiId(rawText)
        val (merchant, senderPerson, receiverPerson) = extractParties(rawText)

        // 4. Balance & Fee Identification
        val balanceCandidate = moneyCandidates.filter { it.role == MoneyRole.BALANCE }
            .maxByOrNull { it.score }
        val balanceAmount = balanceCandidate?.amount

        val feeCandidate = moneyCandidates.filter { it.role == MoneyRole.FEE }
            .maxByOrNull { it.score }
        val feeAmount = feeCandidate?.amount

        // 5. Transaction Amount Identification (Priority Engine)
        val transactionCandidates = moneyCandidates.filter {
            it.role != MoneyRole.BALANCE && it.role != MoneyRole.FEE && it.role != MoneyRole.LIMIT
        }

        val bestTransactionCandidate = transactionCandidates.maxByOrNull { it.score }

        // If no candidate has positive transaction score, check if any non-balance candidate exists
        val selectedCandidate = bestTransactionCandidate
            ?: moneyCandidates.firstOrNull { it.role != MoneyRole.BALANCE && it.role != MoneyRole.FEE }

        val amount = selectedCandidate?.amount ?: 0.0

        // 6. Credit / Debit Direction
        val detectedType = determineTransactionDirection(rawText, selectedCandidate)

        // 7. Confidence Score & Review Assessment
        val (confidence, confidenceReason, requiresReview) = evaluateConfidence(
            selectedCandidate = selectedCandidate,
            allCandidates = moneyCandidates,
            detectedType = detectedType,
            accountLast4 = accountLast4,
            refNumber = refNumber,
            rawText = rawText
        )

        // 8. Suggest Category
        val partyDescription = merchant.ifBlank { senderPerson ?: (receiverPerson ?: (detectedBank ?: "")) }
        val suggestedCategory = CategoryKeywordMatcher.suggestCategory(
            text = "$partyDescription $rawText",
            isIncome = detectedType == DetectedTransactionType.CREDIT
        )

        // 9. Generate Deterministic Stable Fingerprint
        val fingerprint = generateFingerprint(
            bank = detectedBank ?: "Bank",
            accountLast4 = accountLast4,
            amount = amount,
            type = detectedType,
            referenceNumber = refNumber,
            merchant = partyDescription,
            timestamp = timestamp,
            rawText = rawText
        )

        return ParsedTransaction(
            bank = detectedBank,
            accountLast4 = accountLast4,
            detectedType = detectedType,
            amount = amount,
            balanceAfterTransaction = balanceAmount,
            feeAmount = feeAmount,
            merchant = merchant.ifBlank { partyDescription },
            sender = senderPerson,
            receiver = receiverPerson,
            upiId = upiId,
            referenceNumber = refNumber,
            suggestedCategory = suggestedCategory,
            confidence = confidence,
            confidenceScore = selectedCandidate?.score ?: 0f,
            confidenceReason = confidenceReason,
            rawText = rawText,
            normalizedText = normalizedText,
            timestamp = timestamp,
            fingerprint = fingerprint,
            requiresReview = requiresReview,
            allDetectedMoney = moneyCandidates
        )
    }

    /**
     * Normalizes text while preserving original raw message.
     */
    fun normalizeMessage(rawText: String): String {
        var text = rawText
        // Normalize quotation marks
        text = text.replace('“', '"').replace('”', '"').replace('‘', '\'').replace('’', '\'')
        // Normalize unicode spaces to standard ASCII space
        text = text.replace('\u00A0', ' ')
            .replace('\u2007', ' ')
            .replace('\u202F', ' ')
            .replace('\u200B', ' ')
            .replace(Regex("""[\r\n\t]+"""), " ")
        // Normalize currency representations
        text = text.replace("₹", " ₹ ")
            .replace(Regex("""(?i)\b(rs\.?|inr)\b"""), " ₹ ")
        // Collapse spaces
        return text.replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Extracts all monetary values in the message and semantically classifies each.
     */
    fun extractAllMoneyCandidates(rawText: String, normalizedText: String): List<MoneyCandidate> {
        val candidates = mutableListOf<MoneyCandidate>()
        val matcher = NUMBER_TOKEN_PATTERN.matcher(rawText)

        while (matcher.find()) {
            val rawMatch = matcher.group(1) ?: continue
            val start = matcher.start(1)
            val end = matcher.end(1)

            // Validate not an excluded token (e.g. 4-digit year like 2026, or account number, or phone number)
            if (isExcludedToken(rawText, start, end, rawMatch)) {
                continue
            }

            val cleanNumber = rawMatch.replace(",", "").trim()
            val parsedAmount = try {
                BigDecimal(cleanNumber).setScale(2, RoundingMode.HALF_UP).toDouble()
            } catch (e: Exception) {
                continue
            }

            if (parsedAmount <= 0.0) continue

            // Evaluate semantic context surrounding this candidate
            val (role, score, reasoning) = evaluateMoneyContext(rawText, start, end)

            candidates.add(
                MoneyCandidate(
                    rawValue = rawMatch,
                    amount = parsedAmount,
                    startIndex = start,
                    endIndex = end,
                    role = role,
                    score = score,
                    reasoning = reasoning
                )
            )
        }

        return candidates
    }

    /**
     * Checks if a numeric match is actually a year, date, account number, or phone number.
     */
    private fun isExcludedToken(text: String, start: Int, end: Int, token: String): Boolean {
        // Exclude phone numbers (10 digits)
        if (token.length == 10 && !token.contains(".")) return true

        // Exclude 4-digit years (e.g. 2024, 2025, 2026) when near date tokens
        if (token.length == 4 && (token.startsWith("19") || token.startsWith("20"))) {
            val windowStart = (start - 15).coerceAtLeast(0)
            val windowEnd = (end + 15).coerceAtMost(text.length)
            val window = text.substring(windowStart, windowEnd).lowercase(Locale.ENGLISH)
            if (window.contains("jan") || window.contains("feb") || window.contains("mar") ||
                window.contains("apr") || window.contains("may") || window.contains("jun") ||
                window.contains("jul") || window.contains("aug") || window.contains("sep") ||
                window.contains("oct") || window.contains("nov") || window.contains("dec") ||
                window.contains("year") || window.contains("dt") || window.contains("date") ||
                window.contains("-20") || window.contains("/20")
            ) {
                return true
            }
        }

        // Exclude account numbers (preceded by A/C, card, etc.)
        val prefixWindowStart = (start - 20).coerceAtLeast(0)
        val prefix = text.substring(prefixWindowStart, start).lowercase(Locale.ENGLISH)
        if (prefix.contains("a/c") || prefix.contains("acct") || prefix.contains("ending") ||
            prefix.contains("card") || prefix.contains("ending in") || prefix.contains("ending with")
        ) {
            // Only exclude if it looks like an account number without currency symbol
            if (!prefix.contains("₹") && !prefix.contains("rs") && !prefix.contains("inr")) {
                if (token.length in 3..4 && !token.contains(".")) {
                    return true
                }
            }
        }

        // Exclude reference numbers / UTR
        if (prefix.contains("ref") || prefix.contains("rrn") || prefix.contains("utr") || prefix.contains("txn id")) {
            if (!prefix.contains("₹") && !prefix.contains("rs") && !prefix.contains("inr")) {
                return true
            }
        }

        return false
    }

    /**
     * Evaluates the semantic role (TRANSACTION vs BALANCE vs FEE vs LIMIT) and calculates a context score.
     */
    private fun evaluateMoneyContext(text: String, start: Int, end: Int): Triple<MoneyRole, Float, String> {
        val textBefore = text.substring(0, start)
        val lastSentenceBoundary = maxOf(textBefore.lastIndexOf('.'), textBefore.lastIndexOf(';'), textBefore.lastIndexOf('\n'))
        val localBeforeStart = if (lastSentenceBoundary >= 0) lastSentenceBoundary + 1 else (start - 50).coerceAtLeast(0)
        val windowBefore = text.substring(localBeforeStart, start).lowercase(Locale.ENGLISH)

        val textAfter = text.substring(end)
        val firstSentenceBoundary = listOf(textAfter.indexOf('.'), textAfter.indexOf(';'), textAfter.indexOf('\n')).filter { it >= 0 }.minOrNull()
        val localAfterEnd = if (firstSentenceBoundary != null) end + firstSentenceBoundary else (end + 35).coerceAtMost(text.length)
        val windowAfter = text.substring(end, minOf(localAfterEnd, (end + 35).coerceAtMost(text.length))).lowercase(Locale.ENGLISH)

        // 1. Check for Balance context
        val hasTxVerbBefore = CREDIT_KEYWORDS.any { windowBefore.contains(it) } || DEBIT_KEYWORDS.any { windowBefore.contains(it) }
        val isBalanceBefore = BALANCE_KEYWORDS.any { windowBefore.contains(it) }
        val isBalanceAfter = !hasTxVerbBefore &&
                BALANCE_KEYWORDS.any { windowAfter.take(18).contains(it) } &&
                !CREDIT_KEYWORDS.any { windowAfter.contains(it) } &&
                !DEBIT_KEYWORDS.any { windowAfter.contains(it) }

        if (isBalanceBefore || isBalanceAfter) {
            val score = if (isBalanceBefore) 12.0f else 8.0f
            return Triple(MoneyRole.BALANCE, score, "Identified as account balance based on balance keywords")
        }

        // 2. Check for Fee / Charge context
        val isFeeBefore = FEE_KEYWORDS.any { windowBefore.contains(it) }
        val isFeeAfter = !hasTxVerbBefore && FEE_KEYWORDS.any { windowAfter.take(18).contains(it) }
        if (isFeeBefore || isFeeAfter) {
            return Triple(MoneyRole.FEE, 10.0f, "Identified as processing fee or surcharge")
        }

        // 3. Check for Limit context
        val isLimitBefore = LIMIT_KEYWORDS.any { windowBefore.contains(it) }
        if (isLimitBefore) {
            return Triple(MoneyRole.LIMIT, 8.0f, "Identified as credit or spending limit")
        }

        // 4. Transaction Amount Context & Scoring
        var score = 0.0f
        val reasons = mutableListOf<String>()

        // Check currency indicators directly adjacent
        val immediateBefore = text.substring((start - 6).coerceAtLeast(0), start).lowercase(Locale.ENGLISH)
        val immediateAfter = text.substring(end, (end + 6).coerceAtMost(text.length)).lowercase(Locale.ENGLISH)

        if (immediateBefore.contains("₹") || immediateBefore.contains("rs") || immediateBefore.contains("inr")) {
            score += 3.0f
            reasons.add("Preceded by currency indicator")
        }
        if (immediateAfter.contains("₹") || immediateAfter.contains("rs") || immediateAfter.contains("inr")) {
            score += 2.0f
            reasons.add("Followed by currency indicator")
        }

        // Check Credit verbs
        var minCreditDist = Int.MAX_VALUE
        for (kw in CREDIT_KEYWORDS) {
            val idxBefore = windowBefore.lastIndexOf(kw)
            if (idxBefore >= 0) {
                val dist = windowBefore.length - (idxBefore + kw.length)
                if (dist < minCreditDist) minCreditDist = dist
            }
            val idxAfter = windowAfter.indexOf(kw)
            if (idxAfter >= 0 && idxAfter < minCreditDist) {
                minCreditDist = idxAfter
            }
        }
        if (minCreditDist != Int.MAX_VALUE) {
            val proximityBoost = (10.0f - (minCreditDist / 5.0f)).coerceAtLeast(3.0f)
            score += proximityBoost
            reasons.add("Adjacent to credit verb (dist: $minCreditDist)")
        }

        // Check Debit verbs
        var minDebitDist = Int.MAX_VALUE
        for (kw in DEBIT_KEYWORDS) {
            val idxBefore = windowBefore.lastIndexOf(kw)
            if (idxBefore >= 0) {
                val dist = windowBefore.length - (idxBefore + kw.length)
                if (dist < minDebitDist) minDebitDist = dist
            }
            val idxAfter = windowAfter.indexOf(kw)
            if (idxAfter >= 0 && idxAfter < minDebitDist) {
                minDebitDist = idxAfter
            }
        }
        if (minDebitDist != Int.MAX_VALUE) {
            val proximityBoost = (10.0f - (minDebitDist / 5.0f)).coerceAtLeast(3.0f)
            score += proximityBoost
            reasons.add("Adjacent to debit verb (dist: $minDebitDist)")
        }

        // Check for merchant or UPI context
        if (windowBefore.contains("paid to") || windowBefore.contains("sent to") ||
            windowBefore.contains("received from") || windowAfter.contains("to ") ||
            windowBefore.contains("upi") || windowAfter.contains("upi")
        ) {
            score += 2.0f
            reasons.add("Near transaction merchant/UPI context")
        }

        val role = if (score > 1.0f) MoneyRole.TRANSACTION else MoneyRole.OTHER
        val reasoning = if (reasons.isNotEmpty()) reasons.joinToString(", ") else "Generic numeric value"
        return Triple(role, score, reasoning)
    }

    /**
     * Determines transaction direction based on the clause around the selected transaction amount.
     */
    private fun determineTransactionDirection(rawText: String, selectedCandidate: MoneyCandidate?): DetectedTransactionType {
        val lowerText = rawText.lowercase(Locale.ENGLISH)

        // If a transaction candidate was selected, prioritize the local clause around that amount
        if (selectedCandidate != null) {
            val windowStart = (selectedCandidate.startIndex - 45).coerceAtLeast(0)
            val windowEnd = (selectedCandidate.endIndex + 45).coerceAtMost(rawText.length)
            val localClause = rawText.substring(windowStart, windowEnd).lowercase(Locale.ENGLISH)

            val hasCreditInClause = CREDIT_KEYWORDS.any { localClause.contains(it) }
            val hasDebitInClause = DEBIT_KEYWORDS.any { localClause.contains(it) }

            if (hasCreditInClause && !hasDebitInClause) return DetectedTransactionType.CREDIT
            if (hasDebitInClause && !hasCreditInClause) return DetectedTransactionType.DEBIT
        }

        // Global search across entire message
        var firstCreditIndex = Int.MAX_VALUE
        for (phrase in CREDIT_KEYWORDS) {
            val idx = lowerText.indexOf(phrase)
            if (idx in 0 until firstCreditIndex) firstCreditIndex = idx
        }

        var firstDebitIndex = Int.MAX_VALUE
        for (phrase in DEBIT_KEYWORDS) {
            val idx = lowerText.indexOf(phrase)
            if (idx in 0 until firstDebitIndex) firstDebitIndex = idx
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

    /**
     * Calculates confidence level (HIGH, MEDIUM, LOW) and review necessity.
     */
    private fun evaluateConfidence(
        selectedCandidate: MoneyCandidate?,
        allCandidates: List<MoneyCandidate>,
        detectedType: DetectedTransactionType,
        accountLast4: String?,
        refNumber: String,
        rawText: String
    ): Triple<ParseConfidence, String, Boolean> {
        if (selectedCandidate == null || selectedCandidate.amount <= 0.0) {
            return Triple(ParseConfidence.LOW, "No valid transaction amount identified", true)
        }

        if (detectedType == DetectedTransactionType.UNKNOWN) {
            return Triple(ParseConfidence.LOW, "Transaction direction (credit/debit) is unknown", true)
        }

        // Check if there are competing transaction candidates with close scores
        val otherTxCandidates = allCandidates.filter {
            it != selectedCandidate && it.role == MoneyRole.TRANSACTION && it.amount != selectedCandidate.amount
        }
        if (otherTxCandidates.isNotEmpty()) {
            val competing = otherTxCandidates.filter { it.score >= (selectedCandidate.score - 1.5f) }
            if (competing.isNotEmpty()) {
                return Triple(
                    ParseConfidence.LOW,
                    "Ambiguous transaction amount: multiple competing values found",
                    true
                )
            }
        }

        // Score based on richness of identified entities
        var entityScore = 0
        if (accountLast4 != null) entityScore += 2
        if (refNumber.isNotBlank()) entityScore += 2
        if (selectedCandidate.score >= 5.0f) entityScore += 3

        return when {
            entityScore >= 5 -> Triple(ParseConfidence.HIGH, "High confidence with verified entities and clear context", false)
            entityScore >= 3 -> Triple(ParseConfidence.MEDIUM, "Likely transaction; minor field uncertainty", false)
            else -> Triple(ParseConfidence.LOW, "Low confidence: transaction context is sparse", true)
        }
    }

    /**
     * Extracts reference ID, UTR, UPI RRN.
     */
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

    /**
     * Extracts masked account / card ending digits.
     */
    fun extractAccountLast4(text: String): String? {
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

    /**
     * Extracts UPI VPA (e.g. user@okaxis).
     */
    fun extractUpiId(text: String): String? {
        val matcher = VPA_PATTERN.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    /**
     * Extracts merchant, sender, or receiver.
     */
    fun extractParties(text: String): Triple<String, String?, String?> {
        var merchant = ""
        var sender: String? = null
        var receiver: String? = null

        for (pattern in PERSON_OR_MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val match = matcher.group(1)?.trim()
                if (!match.isNullOrEmpty() &&
                    !match.equals("rs", ignoreCase = true) &&
                    !match.equals("inr", ignoreCase = true) &&
                    !match.equals("upi", ignoreCase = true) &&
                    !match.equals("vpa", ignoreCase = true) &&
                    !match.equals("bank", ignoreCase = true)
                ) {
                    val clean = match.trimEnd('.', ',', ':', ';', '-')
                    if (clean.length in 2..30) {
                        merchant = clean
                        break
                    }
                }
            }
        }

        val lower = text.lowercase(Locale.ENGLISH)
        if (lower.contains("received from")) {
            sender = merchant.ifBlank { null }
        } else if (lower.contains("paid to") || lower.contains("sent to")) {
            receiver = merchant.ifBlank { null }
        }

        return Triple(merchant, sender, receiver)
    }

    /**
     * Detects bank or payment app name from sender or message text.
     */
    fun detectBank(sender: String, text: String): String? {
        val upperSender = sender.uppercase(Locale.ENGLISH)
        val upperText = text.uppercase(Locale.ENGLISH)

        for ((bank, keywords) in KNOWN_FINANCIAL_ENTITIES) {
            for (kw in keywords) {
                if (upperSender.contains(kw) || upperText.contains(kw)) {
                    return bank
                }
            }
        }
        return null
    }

    /**
     * Generates a stable deterministic transaction fingerprint.
     */
    fun generateFingerprint(
        bank: String,
        accountLast4: String?,
        amount: Double,
        type: DetectedTransactionType,
        referenceNumber: String,
        merchant: String,
        timestamp: Long,
        rawText: String = ""
    ): String {
        // Priority 1: Bank reference / UTR / UPI Ref
        if (referenceNumber.isNotBlank()) {
            val cleanRef = referenceNumber.trim().uppercase(Locale.ENGLISH)
            return sha256("REF-$cleanRef")
        }

        // Priority 2: Distinct event fingerprint using timestamp & message hash
        val amountFormatted = String.format(Locale.US, "%.2f", amount)
        val normMerchant = merchant.trim().lowercase(Locale.ENGLISH)
        val normAccount = accountLast4 ?: "NOACC"
        val rawHash = if (rawText.isNotEmpty()) sha256(rawText.lowercase(Locale.ENGLISH).replace(Regex("""\s+"""), " ")).take(16) else "NORAW"

        return sha256("EVT-$bank-$normAccount-$amountFormatted-$type-$normMerchant-$timestamp-$rawHash")
    }

    /**
     * Filters out non-financial messages like login OTPs, 2FA, password resets.
     */
    fun isOtpOrNonFinancial(lowerText: String): Boolean {
        if (lowerText.contains("otp") || lowerText.contains("one time password") ||
            lowerText.contains("verification code") || lowerText.contains("security code") ||
            lowerText.contains("do not share this") || lowerText.contains("is your login code") ||
            lowerText.contains("is your secret code")
        ) {
            // Drop unless message explicitly contains debit/credit transaction wording
            if (!lowerText.contains("debited") && !lowerText.contains("credited") && !lowerText.contains("spent")) {
                return true
            }
        }
        return false
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
