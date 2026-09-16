package com.srijayant.spendscope.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Telephony;

import com.srijayant.spendscope.domain.ExpenseParser;
import com.srijayant.spendscope.domain.MerchantSeedData;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.MonthlyReport;

import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class SmsExpenseReader {
    private final ContentResolver contentResolver;
    private final ExpenseParser parser;
    private final CategoryRuleStore categoryRules;
    private final MerchantSeedData seeds;

    public SmsExpenseReader(
            ContentResolver contentResolver,
            ExpenseParser parser,
            CategoryRuleStore categoryRules,
            MerchantSeedData seeds
    ) {
        this.contentResolver = contentResolver;
        this.parser = parser;
        this.categoryRules = categoryRules;
        this.seeds = seeds;
    }

    public MonthlyReport read(YearMonth month) {
        ZoneId zone = ZoneId.systemDefault();
        long startMillis = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();
        long endMillis = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli();

        String[] projection = {
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
        };
        String selection = Telephony.Sms.DATE + " >= ? AND " + Telephony.Sms.DATE + " < ?";
        String[] selectionArgs = {
                Long.toString(startMillis),
                Long.toString(endMillis)
        };

        List<SmsMessage> messages = new ArrayList<>();
        try (Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                Telephony.Sms.DATE + " DESC"
        )) {
            if (cursor == null) {
                return new MonthlyReport(month, new ArrayList<>());
            }

            int idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
            int addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
            int bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
            int dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);

            while (cursor.moveToNext()) {
                messages.add(new SmsMessage(
                        cursor.getLong(idColumn),
                        cursor.getString(addressColumn),
                        cursor.getString(bodyColumn),
                        cursor.getLong(dateColumn)
                ));
            }
        }

        List<Expense> expenses = new ArrayList<>();
        for (SmsMessage message : messages) {
            parser.parse(
                    message.id,
                    message.sender,
                    message.body,
                    message.timestamp,
                    correlatedMerchant(message, messages)
            ).map(categoryRules::apply)
                    .filter(expense -> !expense.isExcludedFromSpend())
                    .ifPresent(expenses::add);
        }
        return new MonthlyReport(month, expenses);
    }

    private String correlatedMerchant(SmsMessage source, List<SmsMessage> messages) {
        for (SmsMessage candidate : messages) {
            if (candidate.id == source.id
                    || Math.abs(candidate.timestamp - source.timestamp) > 10 * 60 * 1000L) {
                continue;
            }
            String body = candidate.body == null ? "" : candidate.body;
            for (MerchantSeedData.RegexRule rule : seeds.getRegexRules()) {
                if (rule.matches(body)) {
                    return body;
                }
            }
        }
        return null;
    }

    private static final class SmsMessage {
        private final long id;
        private final String sender;
        private final String body;
        private final long timestamp;

        private SmsMessage(long id, String sender, String body, long timestamp) {
            this.id = id;
            this.sender = sender;
            this.body = body;
            this.timestamp = timestamp;
        }
    }
}
