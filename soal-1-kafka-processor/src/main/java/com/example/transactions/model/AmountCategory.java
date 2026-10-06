package com.example.transactions.model;

import java.math.BigDecimal;

public enum AmountCategory {
    SMALL,
    MEDIUM,
    LARGE,
    VERY_LARGE;

    private static final BigDecimal HUNDRED_THOUSAND = new BigDecimal("100000");
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");
    private static final BigDecimal TEN_MILLION = new BigDecimal("10000000");

    public static AmountCategory of(BigDecimal amountIdr) {
        if (amountIdr.compareTo(HUNDRED_THOUSAND) < 0) {
            return SMALL;
        }
        if (amountIdr.compareTo(ONE_MILLION) < 0) {
            return MEDIUM;
        }
        if (amountIdr.compareTo(TEN_MILLION) < 0) {
            return LARGE;
        }
        return VERY_LARGE;
    }
}
