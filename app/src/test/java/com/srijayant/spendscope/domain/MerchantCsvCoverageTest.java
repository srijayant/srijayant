package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExpenseClassification;
import com.srijayant.spendscope.model.TransactionType;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MerchantCsvCoverageTest {
    private static final Pattern VPA = Pattern.compile(
            "([A-Z0-9._-]{2,}@[A-Z0-9._-]{2,})",
            Pattern.CASE_INSENSITIVE
    );

    @Test
    public void reviewedMerchantsMeetCoverageTarget() throws IOException {
        CategoryPipeline pipeline = TestMerchantSeeds.pipeline();
        List<String> lines = Files.readAllLines(Path.of("../tools/merchant_review.csv"));
        List<String> failures = new ArrayList<>();
        int checked = 0;

        for (int index = 1; index < lines.size(); index++) {
            String[] columns = lines.get(index).split(",", -1);
            if (columns.length != 4 || columns[1].equals("noise")) {
                continue;
            }
            String merchant = columns[0].trim();
            String bucket = columns[1].trim();
            String expected = columns[3].isBlank()
                    ? columns[2].trim() : columns[3].trim();
            String vpa = findVpa(merchant);
            TransactionType type = bucket.equals("refund")
                    ? TransactionType.REFUND
                    : bucket.equals("cash")
                    ? TransactionType.CASH_WITHDRAWAL : TransactionType.DEBIT;
            ExpenseClassification result = pipeline.classify(new CategoryPipeline.Input(
                    merchant,
                    type,
                    new MerchantExtraction(merchant, vpa, null, bucket.equals("person")),
                    null
            ));
            checked++;
            if (!result.getCategory().getDisplayName().equals(expected)) {
                failures.add(
                        merchant + " expected=" + expected
                                + " actual=" + result.getCategory().getDisplayName()
                                + " source=" + result.getSource()
                );
            }
        }

        double passRate = checked == 0 ? 0.0 : (checked - failures.size()) / (double) checked;
        System.out.printf(
                Locale.ROOT,
                "merchant_review.csv: %d/%d passed (%.2f%%)%n",
                checked - failures.size(),
                checked,
                passRate * 100.0
        );
        failures.forEach(failure -> System.out.println("FAIL " + failure));
        assertTrue(
                "Expected >=95% pass rate; got " + (passRate * 100.0)
                        + "%. Failures: " + failures,
                passRate >= 0.95
        );
    }

    private String findVpa(String merchant) {
        Matcher matcher = VPA.matcher(merchant);
        return matcher.find() ? matcher.group(1) : null;
    }
}
