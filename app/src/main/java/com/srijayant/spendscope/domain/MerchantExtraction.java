package com.srijayant.spendscope.domain;

import java.util.Objects;

public final class MerchantExtraction {
    private final String merchantRaw;
    private final String vpa;
    private final String remark;
    private final boolean p2a;

    public MerchantExtraction(String merchantRaw, String vpa, String remark, boolean p2a) {
        this.merchantRaw = merchantRaw;
        this.vpa = vpa;
        this.remark = remark;
        this.p2a = p2a;
    }

    public String getMerchantRaw() {
        return merchantRaw;
    }

    public String getVpa() {
        return vpa;
    }

    public String getRemark() {
        return remark;
    }

    public boolean isP2A() {
        return p2a;
    }

    public String bestIdentity() {
        if (merchantRaw != null && !merchantRaw.isBlank()) {
            return merchantRaw;
        }
        return Objects.requireNonNullElse(vpa, "");
    }
}
