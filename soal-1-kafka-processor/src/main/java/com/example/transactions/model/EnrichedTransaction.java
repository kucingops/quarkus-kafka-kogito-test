package com.example.transactions.model;

import java.math.BigDecimal;
import java.time.Instant;

public record EnrichedTransaction(
        String transactionId,
        String customerId,
        String maskedEmail,
        String merchant,
        BigDecimal originalAmount,
        String originalCurrency,
        BigDecimal amountIdr,
        String paymentMethod,
        AmountCategory category,
        boolean highRisk,
        Instant transactionTime,
        Instant processedAt) {
}
