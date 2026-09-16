package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.ExpenseCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

final class TestMerchantSeeds {
    private TestMerchantSeeds() {
    }

    static MerchantSeedData load() {
        try {
            return new MerchantSeedParser().parse(
                    new String(
                            Files.readAllBytes(Path.of(
                                    "src/main/assets/seeds/merchant_exact.json"
                            )),
                            StandardCharsets.UTF_8
                    ),
                    new String(
                            Files.readAllBytes(Path.of(
                                    "src/main/assets/seeds/merchants_in.json"
                            )),
                            StandardCharsets.UTF_8
                    )
            );
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    static CategoryPipeline pipeline() {
        CategoryPipeline.RuleLookup noRules = new CategoryPipeline.RuleLookup() {
            @Override
            public Optional<ExpenseCategory> forVpa(String vpa) {
                return Optional.empty();
            }

            @Override
            public Optional<ExpenseCategory> forKey(String normalizedKey) {
                return Optional.empty();
            }
        };
        return new CategoryPipeline(
                new MerchantNormalizer(),
                load(),
                noRules,
                new IdentityRules(
                        List.of("SRIJAYANT", "JAYANT-SRIJAYANTSINGH"),
                        List.of("KRITIKA", "KRITIKAIT09"),
                        true
                )
        );
    }
}
