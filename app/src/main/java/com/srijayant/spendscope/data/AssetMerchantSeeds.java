package com.srijayant.spendscope.data;

import android.content.Context;

import com.srijayant.spendscope.domain.MerchantSeedData;
import com.srijayant.spendscope.domain.MerchantSeedParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class AssetMerchantSeeds {
    private final Context context;

    public AssetMerchantSeeds(Context context) {
        this.context = context.getApplicationContext();
    }

    public MerchantSeedData load() {
        try {
            return new MerchantSeedParser().parse(
                    read("seeds/merchant_exact.json"),
                    read("seeds/merchants_in.json")
            );
        } catch (IOException invalidAssets) {
            throw new IllegalStateException("Bundled merchant seeds could not be loaded", invalidAssets);
        }
    }

    private String read(String path) throws IOException {
        try (InputStream input = context.getAssets().open(path)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8_192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
