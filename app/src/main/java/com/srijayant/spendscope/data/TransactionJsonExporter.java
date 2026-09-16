package com.srijayant.spendscope.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.util.JsonWriter;

import com.srijayant.spendscope.domain.TransactionDeduplicator;
import com.srijayant.spendscope.domain.TransactionMessageParser;
import com.srijayant.spendscope.model.DerivedTransaction;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class TransactionJsonExporter {
    private final ContentResolver contentResolver;
    private final TransactionMessageParser parser;
    private final TransactionDeduplicator deduplicator;

    public TransactionJsonExporter(
            ContentResolver contentResolver,
            TransactionMessageParser parser,
            TransactionDeduplicator deduplicator
    ) {
        this.contentResolver = contentResolver;
        this.parser = parser;
        this.deduplicator = deduplicator;
    }

    public ExportSummary export(Uri destination) throws IOException {
        List<DerivedTransaction> parsedTransactions = new ArrayList<>();
        int scannedMessages = readTransactions(parsedTransactions);
        TransactionDeduplicator.Result result = deduplicator.deduplicate(parsedTransactions);
        writeJson(destination, scannedMessages, parsedTransactions.size(), result);
        return new ExportSummary(
                scannedMessages,
                parsedTransactions.size(),
                result.getTransactions().size(),
                result.getDuplicatesRemoved()
        );
    }

    private int readTransactions(List<DerivedTransaction> destination) {
        String[] projection = {
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
        };
        int scanned = 0;
        try (Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                Telephony.Sms.DATE + " DESC"
        )) {
            if (cursor == null) {
                return 0;
            }
            int idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms._ID);
            int addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS);
            int bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY);
            int dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE);
            while (cursor.moveToNext()) {
                scanned++;
                parser.parse(
                        cursor.getLong(idColumn),
                        cursor.getString(addressColumn),
                        cursor.getString(bodyColumn),
                        cursor.getLong(dateColumn)
                ).ifPresent(destination::add);
            }
        }
        return scanned;
    }

    private void writeJson(
            Uri destination,
            int scannedMessages,
            int parsedMessages,
            TransactionDeduplicator.Result result
    ) throws IOException {
        OutputStream output = contentResolver.openOutputStream(destination, "rwt");
        if (output == null) {
            throw new IOException("Unable to open the selected document");
        }
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
                output,
                StandardCharsets.UTF_8
        ))) {
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("formatVersion").value(1);
            writer.name("generatedAt").value(Instant.now().toString());
            writer.name("currency").value("INR");
            writer.name("rawSmsBodiesIncluded").value(false);
            writer.name("sourceMessagesScanned").value(scannedMessages);
            writer.name("transactionMessagesParsed").value(parsedMessages);
            writer.name("uniqueTransactions").value(result.getTransactions().size());
            writer.name("duplicateMessagesRemoved").value(result.getDuplicatesRemoved());
            writer.name("transactions").beginArray();
            for (DerivedTransaction transaction : result.getTransactions()) {
                writeTransaction(writer, transaction);
            }
            writer.endArray();
            writer.endObject();
        }
    }

    private void writeTransaction(JsonWriter writer, DerivedTransaction transaction)
            throws IOException {
        writer.beginObject();
        writer.name("smsId").value(transaction.getSmsId());
        writer.name("sender").value(transaction.getSender());
        writer.name("timestamp").value(transaction.getTimestamp().toString());
        writer.name("type").value(transaction.getType().name());
        writer.name("amountPaise").value(transaction.getAmountPaise());
        writer.name("amount").value(
                BigDecimal.valueOf(transaction.getAmountPaise(), 2).toPlainString()
        );
        writer.name("instrument").value(transaction.getInstrument());
        writeNullable(writer, "accountLast4", transaction.getAccountLast4());
        writeNullable(writer, "merchantOrPayee", transaction.getMerchant());
        writeNullable(writer, "vpa", transaction.getVpa());
        writeNullable(writer, "reference", transaction.getReference());
        if (transaction.getBalancePaise() == null) {
            writer.name("balancePaise").nullValue();
            writer.name("balance").nullValue();
        } else {
            writer.name("balancePaise").value(transaction.getBalancePaise());
            writer.name("balance").value(
                    BigDecimal.valueOf(transaction.getBalancePaise(), 2).toPlainString()
            );
        }
        writer.name("suggestedCategory").value(transaction.getSuggestedCategory());
        writer.name("categorySource").value(transaction.getCategorySource());
        writer.name("confidence").value(
                transaction.getConfidence().name().toLowerCase(java.util.Locale.ROOT)
        );
        writer.name("duplicateSmsCount").value(transaction.getDuplicateMessages());
        writer.endObject();
    }

    private void writeNullable(JsonWriter writer, String name, String value) throws IOException {
        writer.name(name);
        if (value == null || value.isBlank()) {
            writer.nullValue();
        } else {
            writer.value(value);
        }
    }

    public static final class ExportSummary {
        private final int scannedMessages;
        private final int parsedMessages;
        private final int uniqueTransactions;
        private final int duplicatesRemoved;

        private ExportSummary(
                int scannedMessages,
                int parsedMessages,
                int uniqueTransactions,
                int duplicatesRemoved
        ) {
            this.scannedMessages = scannedMessages;
            this.parsedMessages = parsedMessages;
            this.uniqueTransactions = uniqueTransactions;
            this.duplicatesRemoved = duplicatesRemoved;
        }

        public int getScannedMessages() {
            return scannedMessages;
        }

        public int getParsedMessages() {
            return parsedMessages;
        }

        public int getUniqueTransactions() {
            return uniqueTransactions;
        }

        public int getDuplicatesRemoved() {
            return duplicatesRemoved;
        }
    }
}
