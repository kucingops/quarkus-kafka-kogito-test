package com.example.checkout.persistence;

import java.time.Instant;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity extends PanacheEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    public String orderNumber;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "item_count", nullable = false)
    public int itemCount;

    public long subtotal;

    @Column(name = "shipping_fee")
    public long shippingFee;

    public long discount;

    public long total;

    @Column(name = "payment_method")
    public String paymentMethod;

    @Column(name = "payment_reference")
    public String paymentReference;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    public static OrderEntity findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResult();
    }
}
