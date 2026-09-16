package com.srijayant.expense.data

/**
 * Parses bank / UPI debit SMS into structured expenses.
 * Tuned for common Indian bank alert formats (HDFC, SBI, ICICI, Axis, Kotak, Paytm, PhonePe, GPay).
 */
object ExpenseParser {

    private val debitKeywords = listOf(
        "debited", "spent", "paid", "purchase", "withdrawn",
        "withdrawal", "sent", "transferred to", "payment of",
        "txn of", "transaction of", "dr ", " dr."
    )

    private val creditKeywords = listOf(
        "credited", "received", "deposited", "refund", "cashback", "cr "
    )

    private val amountPatterns = listOf(
        Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""),
        Regex("""(?i)(?:amount|amt|of)\s*(?:rs\.?|inr|₹)?\s*([0-9]{1,3}(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"""),
        Regex("""(?i)(?:debited|spent|paid|sent)\s*(?:by|for|with)?\s*(?:rs\.?|inr|₹)?\s*([0-9,]+\.?[0-9]*)""")
    )

    private val merchantPatterns = listOf(
        Regex("""(?i)(?:at|to|towards|for)\s+([A-Za-z0-9&.\-\s]{2,40}?)(?:\s+on\s|\s+via\s|\s+using\s|\s+upi|\s+ref|\s+avl|\.|,|$)"""),
        Regex("""(?i)(?:VPA|UPI ID)[:\s]+([A-Za-z0-9.\-_@]+)"""),
        Regex("""(?i)(?:Info|Merchant)[:\s]+([A-Za-z0-9&.\-\s]{2,40})""")
    )

    private val categoryRules: List<Pair<ExpenseCategory, List<String>>> = listOf(
        ExpenseCategory.FOOD to listOf(
            "swiggy", "zomato", "dominos", "mcdonald", "kfc", "starbucks",
            "cafe", "restaurant", "dining", "food", "blinkit", "zepto", "instamart"
        ),
        ExpenseCategory.SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa",
            "reliance", "dmart", "bigbasket", "store", "mart"
        ),
        ExpenseCategory.TRAVEL to listOf(
            "uber", "ola", "rapido", "irctc", "makemytrip", "goibibo",
            "indigo", "airindia", "petrol", "fuel", "parking", "metro", "fastag"
        ),
        ExpenseCategory.BILLS to listOf(
            "electricity", "bses", "tata power", "airtel", "jio", "vi ",
            "vodafone", "broadband", "gas", "water", "recharge", "billdesk",
            "rent", "emi", "insurance", "premium"
        ),
        ExpenseCategory.ENTERTAINMENT to listOf(
            "netflix", "spotify", "prime", "hotstar", "disney", "youtube",
            "bookmyshow", "pvr", "inox", "sony liv"
        ),
        ExpenseCategory.HEALTH to listOf(
            "pharmacy", "apollo", "1mg", "pharmeasy", "hospital", "clinic",
            "diagnostic", "medlife", "netmeds"
        ),
        ExpenseCategory.ATM to listOf("atm", "cash wdl", "cash withdrawal", "withdrawn"),
        ExpenseCategory.TRANSFER to listOf("neft", "imps", "rtgs", "upi/", "transfer", "sent to")
    )

    data class ParsedSms(
        val amount: Double,
        val merchant: String,
        val category: ExpenseCategory,
        val isDebit: Boolean
    )

    fun parse(body: String, sender: String = ""): ParsedSms? {
        val text = body.replace('\n', ' ').trim()
        if (text.isBlank()) return null

        val lower = text.lowercase()
        val looksLikeDebit = debitKeywords.any { lower.contains(it) }
        val looksLikeCredit = creditKeywords.any { lower.contains(it) }

        // Prefer debits; skip pure credits (income/refunds) for expense tracking
        if (!looksLikeDebit && !looksLikeCredit) {
            // Still try if amount + bank-like sender
            if (!isLikelyBankSender(sender) || extractAmount(text) == null) return null
        }
        if (looksLikeCredit && !looksLikeDebit) return null

        val amount = extractAmount(text) ?: return null
        if (amount <= 0.0 || amount > 10_000_000) return null

        val merchant = extractMerchant(text) ?: guessMerchantFromSender(sender)
        val category = categorize(merchant, text)

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            category = category,
            isDebit = true
        )
    }

    private fun extractAmount(text: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues.getOrNull(1)?.replace(",", "") ?: continue
            val value = raw.toDoubleOrNull() ?: continue
            if (value > 0) return value
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text) ?: continue
            val raw = match.groupValues.getOrNull(1)?.trim().orEmpty()
            val cleaned = raw
                .replace(Regex("""\s+"""), " ")
                .trim(' ', '.', ',', '-', ':')
            if (cleaned.length in 2..40 && !cleaned.equals("your", ignoreCase = true)) {
                return cleaned.replaceFirstChar { it.titlecase() }
            }
        }
        return null
    }

    private fun guessMerchantFromSender(sender: String): String {
        val s = sender.uppercase()
        return when {
            "HDFC" in s -> "HDFC Bank"
            "SBI" in s || "SBIIN" in s -> "SBI"
            "ICICI" in s -> "ICICI Bank"
            "AXIS" in s -> "Axis Bank"
            "KOTAK" in s -> "Kotak Bank"
            "PAYTM" in s -> "Paytm"
            "PHONPE" in s || "PHONEPE" in s -> "PhonePe"
            "GPAY" in s || "GOOGLE" in s -> "Google Pay"
            "BOB" in s || "BARODA" in s -> "Bank of Baroda"
            "PNB" in s -> "PNB"
            else -> if (sender.isNotBlank()) sender else "Unknown"
        }
    }

    private fun isLikelyBankSender(sender: String): Boolean {
        val s = sender.uppercase()
        return listOf(
            "HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PAYTM", "PHONPE",
            "PHONEPE", "GPAY", "BOB", "PNB", "IDFC", "YESBK", "INDUS",
            "BANK", "UPI", "AMEX", "CITI"
        ).any { it in s }
    }

    fun categorize(merchant: String, rawMessage: String): ExpenseCategory {
        val haystack = "$merchant $rawMessage".lowercase()
        for ((category, keywords) in categoryRules) {
            if (keywords.any { haystack.contains(it) }) return category
        }
        return ExpenseCategory.OTHER
    }
}
