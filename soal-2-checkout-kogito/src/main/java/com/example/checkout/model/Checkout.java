package com.example.checkout.model;

import java.util.ArrayList;
import java.util.List;

public class Checkout {

    private String requestId;
    private String customerId;
    private String customerEmail;
    private List<CartItem> items = new ArrayList<>();
    private String paymentMethod;
    private String voucherCode;

    private CheckoutStatus status = CheckoutStatus.NEW;
    private String failureReason;

    private boolean cartValid;
    private boolean duplicateRequest;
    private boolean stockReserved;
    private boolean paymentSuccess;

    private long subtotal;
    private long shippingFee;
    private long discount;
    private long total;

    private String paymentReference;
    private String orderNumber;
    private boolean notificationSent;

    public Checkout() {
    }

    public void fail(CheckoutStatus failedStatus, String reason) {
        this.status = failedStatus;
        this.failureReason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public CheckoutStatus getStatus() {
        return status;
    }

    public void setStatus(CheckoutStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean isCartValid() {
        return cartValid;
    }

    public void setCartValid(boolean cartValid) {
        this.cartValid = cartValid;
    }

    public boolean isDuplicateRequest() {
        return duplicateRequest;
    }

    public void setDuplicateRequest(boolean duplicateRequest) {
        this.duplicateRequest = duplicateRequest;
    }

    public boolean isStockReserved() {
        return stockReserved;
    }

    public void setStockReserved(boolean stockReserved) {
        this.stockReserved = stockReserved;
    }

    public boolean isPaymentSuccess() {
        return paymentSuccess;
    }

    public void setPaymentSuccess(boolean paymentSuccess) {
        this.paymentSuccess = paymentSuccess;
    }

    public long getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(long subtotal) {
        this.subtotal = subtotal;
    }

    public long getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(long shippingFee) {
        this.shippingFee = shippingFee;
    }

    public long getDiscount() {
        return discount;
    }

    public void setDiscount(long discount) {
        this.discount = discount;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }
}
