package com.srijayant.smsexpense.data

/**
 * Parses bank / UPI / card transaction SMS into [Transaction]s.
 *
 * Handles the common formats used by Indian banks (HDFC, ICICI, SBI, Axis,
 * Kotak, ...), card networks and UPI apps, e.g.:
 *  - "Rs.450.00 debited from A/c XX1234 on 12-09-26 to VPA swiggy.upi@icici ..."
 *  - "INR 2,499.00 spent on HDFC Bank Card xx1234 at AMAZON on 2026-09-12"
 *  - "Your A/c XX9876 credited with Rs.50,000.00 by a/c linked to VPA acme@ybl"
 *
 * OTPs, promotional offers, payment requests and failed/reversed transactions
 * are rejected.
 */
object TransactionParser {

    private val amountRegex = Regex(
        """(?:rs\.?|inr|₹)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "payment of", "purchase of",
        "purchased", "withdrawn", "deducted", "sent", "txn of"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refunded", "refund of",
        "cashback of"
    )

    private val rejectKeywords = listOf(
        // OTP / verification
        "otp", "one time password", "verification code", "do not share",
        // requests & future debits
        "has requested", "requested money", "payment request", "will be debited",
        "will be deducted", "due on", "is due", "e-mandate", "autopay is set",
        // failures / reversals
        "failed", "declined", "could not be processed", "reversed", "reversal",
        // marketing
        "offer", "cashback up to", "apply now", "loan", "win ", "congratulations",
        "emi starting", "pre-approved", "insurance plan"
    )

    private val merchantPatterns = listOf(
        // "to VPA merchant@bank" / "from VPA payer@bank"
        Regex("""(?:to|from)\s+(?:vpa\s+)?([a-z0-9._\-]+@[a-z]+)""", RegexOption.IGNORE_CASE),
        // "at MERCHANT on" (card transactions)
        Regex("""\bat\s+([A-Za-z0-9&*'._\- ]{2,40}?)\s+(?:on|via|using|ref)\b""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+([A-Za-z0-9&*'._\- ]{2,40}?)[.,](?:\s|$)""", RegexOption.IGNORE_CASE),
        // "to MERCHANT on/via" (UPI app style)
        Regex("""\b(?:to|towards)\s+([A-Za-z0-9&*'._\- ]{2,40}?)\s+(?:on|via|using|ref)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:to|towards)\s+([A-Za-z0-9&*'._\- ]{2,40}?)[.,](?:\s|$)""", RegexOption.IGNORE_CASE),
        // "from SENDER on" (credits)
        Regex("""\bfrom\s+([A-Za-z0-9&*'._\- ]{2,40}?)\s+(?:on|via|ref)\b""", RegexOption.IGNORE_CASE),
        // "Info: MERCHANT" / "Info- MERCHANT"
        Regex("""\binfo[:\-]\s*([A-Za-z0-9&*'._\- @]{2,40}?)(?:[.,]|$)""", RegexOption.IGNORE_CASE)
    )

    /** Senders that are plain mobile numbers are people, not banks. */
    private val personalNumberRegex = Regex("""^\+?[0-9\- ]{10,15}$""")

    fun parse(sms: SmsMessage): Transaction? {
        if (personalNumberRegex.matches(sms.sender.trim())) return null

        val body = sms.body
        val lower = body.lowercase()

        if (rejectKeywords.any { lower.contains(it) }) return null

        val amountMatch = amountRegex.find(body) ?: return null
        val amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val type = detectType(lower) ?: return null
        val merchant = extractMerchant(body) ?: fallbackMerchant(sms.sender)
        val category = Categorizer.categorize(merchant, body, type)

        return Transaction(
            amount = amount,
            type = type,
            merchant = merchant,
            category = category,
            sender = sms.sender,
            timestampMillis = sms.timestampMillis,
            rawBody = body
        )
    }

    /**
     * Picks the transaction direction from whichever keyword appears first,
     * which matters for messages like "debited from A/c ... and credited to
     * beneficiary".
     */
    private fun detectType(lowerBody: String): TransactionType? {
        val firstDebit = debitKeywords.mapNotNull {
            lowerBody.indexOf(it).takeIf { i -> i >= 0 }
        }.minOrNull()
        val firstCredit = creditKeywords.mapNotNull {
            lowerBody.indexOf(it).takeIf { i -> i >= 0 }
        }.minOrNull()

        return when {
            firstDebit != null && (firstCredit == null || firstDebit <= firstCredit) -> TransactionType.DEBIT
            firstCredit != null -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun extractMerchant(body: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(body) ?: continue
            val candidate = match.groupValues[1].trim().trimEnd('.', ',')
            if (candidate.length < 2) continue
            // Skip captures that are just account/card references.
            if (Regex("""^(?:a/c|ac|acct|account|card|your|bank)\b""", RegexOption.IGNORE_CASE)
                    .containsMatchIn(candidate)
            ) continue
            return cleanMerchant(candidate)
        }
        return null
    }

    private fun cleanMerchant(raw: String): String {
        // "swiggy.upi@icici" -> "swiggy.upi", then title-case words.
        val base = raw.substringBefore('@').replace(Regex("""[_*]+"""), " ").trim()
        return base.split(Regex("""\s+""")).joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
    }

    private fun fallbackMerchant(sender: String): String {
        // "VM-HDFCBK-S" -> "HDFCBK"
        val core = sender.split('-').maxByOrNull { it.length } ?: sender
        return core.ifBlank { "Unknown" }
    }
}
