package com.example.checkout.entity;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "checkout_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = { "customer_id", "request_id" }))
public class CheckoutRequestEntity extends PanacheEntity {

    @Column(name = "request_id", nullable = false)
    public String requestId;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
