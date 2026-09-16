package com.srijayant.spendscope.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;

public final class MerchantRuleKey {
    private MerchantRuleKey() {
    }

    public static boolean isEligibleMerchant(String merchant) {
        if (merchant == null || merchant.isBlank()
                || merchant.equalsIgnoreCase("Transaction alert")) {
            return false;
        }
        String trimmed = merchant.trim();
        boolean looksLikeSmsSenderId = trimmed.equals(trimmed.toUpperCase(Locale.ROOT))
                && trimmed.matches("[A-Z0-9 -]{5,16}");
        return !looksLikeSmsSenderId;
    }

    public static String fromMerchant(String merchant) {
        String normalized = Normalizer.normalize(
                        merchant == null ? "" : merchant,
                        Normalizer.Form.NFKC
                )
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return sha256(normalized);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(Character.forDigit((item >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(item & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
