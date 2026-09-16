package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.DerivedTransaction;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExpenseClassification;
import com.srijayant.spendscope.model.TransactionType;

import java.util.Optional;

public final class ExpenseParser {
    private final TransactionMessageParser transactionParser;

    public ExpenseParser() {
        this(new TransactionMessageParser());
    }

    public ExpenseParser(TransactionMessageParser transactionParser) {
        this.transactionParser = transactionParser;
    }

    public Optional<Expense> parse(
            long messageId,
            String sender,
            String body,
            long timestampMillis
    ) {
        return parse(messageId, sender, body, timestampMillis, null);
    }

    public Optional<Expense> parse(
            long messageId,
            String sender,
            String body,
            long timestampMillis,
            String correlatedMerchant
    ) {
        Optional<DerivedTransaction> parsed = transactionParser.parse(
                messageId,
                sender,
                body,
                timestampMillis,
                correlatedMerchant
        );
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        DerivedTransaction transaction = parsed.get();
        if (transaction.getType() != TransactionType.DEBIT
                && transaction.getType() != TransactionType.CASH_WITHDRAWAL
                && transaction.getType() != TransactionType.FAMILY_TRANSFER) {
            return Optional.empty();
        }
        ExpenseCategory category = ExpenseCategory.fromDisplayName(
                transaction.getSuggestedCategory()
        ).orElse(ExpenseCategory.OTHER);
        ExpenseClassification classification = new ExpenseClassification(
                category,
                transaction.getConfidence(),
                com.srijayant.spendscope.model.ClassificationSource.valueOf(
                        transaction.getCategorySource()
                ),
                transaction.needsReview(),
                transaction.isExcludedFromSpend()
        );
        return Optional.of(new Expense(
                messageId,
                transaction.getAmountPaise(),
                transaction.getMerchant() == null
                        ? "Transaction alert" : transaction.getMerchant(),
                transaction.getVpa(),
                classification,
                transaction.getTimestamp()
        ));
    }
}
