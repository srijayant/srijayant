package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.ExpenseCategory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MerchantSeedParser {
    private static final String JSON_STRING = "((?:\\\\.|[^\"\\\\])*)";
    private static final Pattern EXACT_ENTRY = Pattern.compile(
            "\"" + JSON_STRING + "\"\\s*:\\s*\\{\\s*"
                    + "\"category\"\\s*:\\s*\"" + JSON_STRING + "\"\\s*,\\s*"
                    + "\"bucket\"\\s*:\\s*\"" + JSON_STRING + "\"\\s*}"
    );
    private static final Pattern REGEX_ENTRY = Pattern.compile(
            "\"category\"\\s*:\\s*\"" + JSON_STRING + "\"\\s*,\\s*"
                    + "\"pattern\"\\s*:\\s*\"" + JSON_STRING + "\""
    );

    public MerchantSeedData parse(String exactJson, String regexJson) {
        Map<String, MerchantSeedData.Entry> exact = new LinkedHashMap<>();
        Matcher exactMatcher = EXACT_ENTRY.matcher(exactJson == null ? "" : exactJson);
        while (exactMatcher.find()) {
            String key = unescape(exactMatcher.group(1));
            ExpenseCategory.fromDisplayName(unescape(exactMatcher.group(2))).ifPresent(category ->
                    exact.put(key, new MerchantSeedData.Entry(
                            category,
                            unescape(exactMatcher.group(3))
                    ))
            );
        }

        List<MerchantSeedData.RegexRule> regexRules = new ArrayList<>();
        Matcher regexMatcher = REGEX_ENTRY.matcher(regexJson == null ? "" : regexJson);
        while (regexMatcher.find()) {
            ExpenseCategory.fromDisplayName(unescape(regexMatcher.group(1))).ifPresent(category ->
                    regexRules.add(new MerchantSeedData.RegexRule(
                            category,
                            unescape(regexMatcher.group(2))
                    ))
            );
        }
        return new MerchantSeedData(exact, regexRules);
    }

    private String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (!escaped && character == '\\') {
                escaped = true;
                continue;
            }
            if (escaped) {
                switch (character) {
                    case 'n':
                        result.append('\n');
                        break;
                    case 'r':
                        result.append('\r');
                        break;
                    case 't':
                        result.append('\t');
                        break;
                    case 'b':
                        result.append('\b');
                        break;
                    case 'f':
                        result.append('\f');
                        break;
                    default:
                        result.append(character);
                        break;
                }
                escaped = false;
            } else {
                result.append(character);
            }
        }
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }
}
