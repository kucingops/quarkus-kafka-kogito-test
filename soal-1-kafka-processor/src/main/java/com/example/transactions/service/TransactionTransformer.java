package com.example.transactions.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.model.EnrichedTransaction;
import com.example.transactions.model.RawTransaction;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionTransformer {

    static final Map<String, BigDecimal> RATES_TO_IDR = Map.of(
            "IDR", BigDecimal.ONE,
            "USD", new BigDecimal("16000"),
            "SGD", new BigDecimal("12000"),
            "EUR", new BigDecimal("17500"));

    static final BigDecimal HIGH_RISK_THRESHOLD_IDR = new BigDecimal("10000000");

    private final Clock clock;

    public TransactionTransformer() {
        this(Clock.systemUTC());
    }

    TransactionTransformer(Clock clock) {
        this.clock = clock;
    }

    public EnrichedTransaction transform(RawTransaction raw) {
        validate(raw);

        String currency = raw.currency().trim().toUpperCase(Locale.ROOT);
        BigDecimal rate = RATES_TO_IDR.get(currency);
        if (rate == null) {
            throw new InvalidTransactionException("unsupported currency: " + raw.currency());
        }

        BigDecimal amountIdr = raw.amount().multiply(rate).setScale(0, RoundingMode.HALF_UP);

        return new EnrichedTransaction(
                raw.transactionId().trim(),
                raw.customerId().trim(),
                maskEmail(raw.customerEmail()),
                normalizeMerchant(raw.merchant()),
                raw.amount(),
                currency,
                amountIdr,
                normalizePaymentMethod(raw.paymentMethod()),
                AmountCategory.of(amountIdr),
                amountIdr.compareTo(HIGH_RISK_THRESHOLD_IDR) >= 0,
                raw.timestamp(),
                Instant.now(clock));
    }

    private void validate(RawTransaction raw) {
        if (raw == null) {
            throw new InvalidTransactionException("empty payload");
        }
        if (isBlank(raw.transactionId())) {
            throw new InvalidTransactionException("transactionId is required");
        }
        if (isBlank(raw.customerId())) {
            throw new InvalidTransactionException("customerId is required");
        }
        if (isBlank(raw.merchant())) {
            throw new InvalidTransactionException("merchant is required");
        }
        if (raw.amount() == null || raw.amount().signum() <= 0) {
            throw new InvalidTransactionException("amount must be greater than 0");
        }
        if (isBlank(raw.currency())) {
            throw new InvalidTransactionException("currency is required");
        }
        if (raw.timestamp() == null) {
            throw new InvalidTransactionException("timestamp is required");
        }
    }

    static String normalizeMerchant(String merchant) {
        return merchant.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    static String normalizePaymentMethod(String method) {
        if (isBlank(method)) {
            return "UNKNOWN";
        }
        return method.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    static String maskEmail(String email) {
        if (isBlank(email)) {
            return null;
        }
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = value.substring(0, at);
        String domain = value.substring(at);
        int visible = local.length() <= 2 ? 1 : 2;
        return local.substring(0, visible) + "*".repeat(local.length() - visible) + domain;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
