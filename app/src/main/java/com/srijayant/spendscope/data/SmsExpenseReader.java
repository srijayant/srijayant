package com.srijayant.spendscope.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.Telephony;

import com.srijayant.spendscope.domain.ExpenseParser;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.MonthlyReport;

import java.time.ZoneId;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class SmsExpenseReader {
    private final ContentResolver contentResolver;
    private final ExpenseParser parser;

    public SmsExpenseReader(ContentResolver contentResolver, ExpenseParser parser) {
        this.contentResolver = contentResolver;
        this.parser = parser;
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

        List<Expense> expenses = new ArrayList<>();
        try (Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                Telephony.Sms.DATE + " DESC"
        )) {
            if (cursor == null) {
                return new MonthlyReport(month, expenses);
            }

            int idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
            int addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
            int bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
            int dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);

            while (cursor.moveToNext()) {
                parser.parse(
                        cursor.getLong(idColumn),
                        cursor.getString(addressColumn),
                        cursor.getString(bodyColumn),
                        cursor.getLong(dateColumn)
                ).ifPresent(expenses::add);
            }
        }

        return new MonthlyReport(month, expenses);
    }
}
