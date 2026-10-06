package com.example.checkout.persistence;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "checkout_requests")
public class CheckoutRequestEntity extends PanacheEntity {

    @Column(name = "request_id", nullable = false, unique = true)
    public String requestId;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static boolean existsByRequestId(String requestId) {
        return count("requestId", requestId) > 0;
    }
}
