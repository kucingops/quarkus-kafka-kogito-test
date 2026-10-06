package com.example.checkout.model;

public enum CheckoutStatus {
    NEW,
    VALIDATED,
    STOCK_RESERVED,
    PRICED,
    PAID,
    COMPLETED,

    REJECTED_INVALID_CART,
    REJECTED_DUPLICATE_REQUEST,
    REJECTED_OUT_OF_STOCK,
    PAYMENT_FAILED,
    ORDER_FAILED
}
