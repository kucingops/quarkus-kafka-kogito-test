package com.example.transactions.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.transactions.model.AmountCategory;
import com.example.transactions.model.EnrichedTransaction;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class TransactionEntity extends PanacheEntityBase {

    @Id
    @Column(name = "transaction_id", length = 64)
    public String transactionId;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "masked_email")
    public String maskedEmail;

    @Column(nullable = false)
    public String merchant;

    @Column(name = "original_amount", nullable = false, precision = 19, scale = 2)
    public BigDecimal originalAmount;

    @Column(name = "original_currency", nullable = false, length = 3)
    public String originalCurrency;

    @Column(name = "amount_idr", nullable = false, precision = 19, scale = 0)
    public BigDecimal amountIdr;

    @Column(name = "payment_method")
    public String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public AmountCategory category;

    @Column(name = "high_risk", nullable = false)
    public boolean highRisk;

    @Column(name = "transaction_time", nullable = false)
    public Instant transactionTime;

    @Column(name = "processed_at", nullable = false)
    public Instant processedAt;

    public static TransactionEntity from(EnrichedTransaction tx) {
        TransactionEntity e = new TransactionEntity();
        e.transactionId = tx.transactionId();
        e.customerId = tx.customerId();
        e.maskedEmail = tx.maskedEmail();
        e.merchant = tx.merchant();
        e.originalAmount = tx.originalAmount();
        e.originalCurrency = tx.originalCurrency();
        e.amountIdr = tx.amountIdr();
        e.paymentMethod = tx.paymentMethod();
        e.category = tx.category();
        e.highRisk = tx.highRisk();
        e.transactionTime = tx.transactionTime();
        e.processedAt = tx.processedAt();
        return e;
    }
}
