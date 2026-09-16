package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.DerivedTransaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MerchantListBuilder {
    public List<String> build(List<DerivedTransaction> transactions) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (DerivedTransaction transaction : transactions) {
            String merchant = transaction.getMerchant();
            if (merchant == null || merchant.isBlank()) {
                continue;
            }
            unique.putIfAbsent(
                    MerchantRuleKey.fromMerchant(merchant),
                    merchant.toUpperCase(Locale.ROOT)
            );
        }
        List<String> merchants = new ArrayList<>(unique.values());
        merchants.sort(String.CASE_INSENSITIVE_ORDER);
        return merchants;
    }
}
