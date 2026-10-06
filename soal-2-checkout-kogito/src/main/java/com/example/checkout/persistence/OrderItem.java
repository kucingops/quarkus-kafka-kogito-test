package com.example.checkout.persistence;

import com.example.checkout.model.CartItem;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class OrderItem {

    @Column(nullable = false)
    public String sku;

    @Column(name = "product_name")
    public String productName;

    public int quantity;

    @Column(name = "unit_price")
    public long unitPrice;

    @Column(name = "line_total")
    public long lineTotal;

    public static OrderItem from(CartItem item) {
        OrderItem line = new OrderItem();
        line.sku = item.getSku();
        line.productName = item.getProductName();
        line.quantity = item.getQuantity();
        line.unitPrice = item.getUnitPrice();
        line.lineTotal = item.getLineTotal();
        return line;
    }
}
