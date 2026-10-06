package com.example.checkout.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity extends PanacheEntity {

    @Column(name = "order_number", nullable = false, unique = true)
    public String orderNumber;

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "request_id")
    public String requestId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_no")
    public List<OrderItem> items = new ArrayList<>();

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

    public static OrderEntity findByRequest(String customerId, String requestId) {
        return find("customerId = ?1 and requestId = ?2", customerId, requestId).firstResult();
    }
}
