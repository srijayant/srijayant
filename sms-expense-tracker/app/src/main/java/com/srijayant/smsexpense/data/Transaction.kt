package com.srijayant.smsexpense.data

enum class TransactionType { DEBIT, CREDIT }

enum class Category(val label: String) {
    FOOD("Food & Dining"),
    GROCERIES("Groceries"),
    SHOPPING("Shopping"),
    TRAVEL("Travel"),
    FUEL("Fuel"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    EDUCATION("Education"),
    RENT("Rent"),
    INVESTMENT("Investment"),
    TRANSFER("Transfers"),
    ATM("ATM / Cash"),
    OTHER("Other")
}

/**
 * A financial transaction extracted from a single SMS.
 */
data class Transaction(
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val category: Category,
    val sender: String,
    val timestampMillis: Long,
    val rawBody: String
)

/** A raw SMS as read from the device inbox. */
data class SmsMessage(
    val sender: String,
    val body: String,
    val timestampMillis: Long
)
