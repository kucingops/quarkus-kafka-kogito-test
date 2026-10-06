package com.example.transactions.model;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawTransaction(
        String transactionId,
        String customerId,
        String customerEmail,
        String merchant,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        Instant timestamp) {
}
