package com.example.transactions.repository;

import java.math.BigDecimal;

import com.example.transactions.model.AmountCategory;

public record CategorySummary(AmountCategory category, Long count, BigDecimal totalIdr) {
}
