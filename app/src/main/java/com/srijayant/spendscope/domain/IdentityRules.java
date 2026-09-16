package com.srijayant.spendscope.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class IdentityRules {
    private final List<String> selfIdentities;
    private final List<String> familyIdentities;
    private final boolean familyExcludedFromSpend;

    public IdentityRules(
            List<String> selfIdentities,
            List<String> familyIdentities,
            boolean familyExcludedFromSpend
    ) {
        this.selfIdentities = normalizedCopy(selfIdentities);
        this.familyIdentities = normalizedCopy(familyIdentities);
        this.familyExcludedFromSpend = familyExcludedFromSpend;
    }

    public boolean isSelf(MerchantExtraction extraction) {
        return containsIdentity(extraction, selfIdentities);
    }

    public boolean isFamily(MerchantExtraction extraction) {
        return containsIdentity(extraction, familyIdentities);
    }

    public List<String> getSelfIdentities() {
        return selfIdentities;
    }

    public List<String> getFamilyIdentities() {
        return familyIdentities;
    }

    public boolean isFamilyExcludedFromSpend() {
        return familyExcludedFromSpend;
    }

    private boolean containsIdentity(MerchantExtraction extraction, List<String> identities) {
        String value = (
                nullToEmpty(extraction.getMerchantRaw()) + " "
                        + nullToEmpty(extraction.getVpa())
        ).toUpperCase(Locale.ROOT);
        for (String identity : identities) {
            if (value.contains(identity)) {
                return true;
            }
        }
        return false;
    }

    private List<String> normalizedCopy(List<String> source) {
        List<String> normalized = new ArrayList<>();
        if (source != null) {
            for (String value : source) {
                if (value != null && !value.isBlank()) {
                    String item = value.trim().toUpperCase(Locale.ROOT);
                    if (!normalized.contains(item)) {
                        normalized.add(item);
                    }
                }
            }
        }
        return Collections.unmodifiableList(normalized);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
