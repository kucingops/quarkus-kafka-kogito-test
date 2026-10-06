package com.example.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;

class PaymentServiceTest {

    private final PaymentService service = new PaymentService();

    private static Checkout checkout(String method, long total) {
        Checkout c = new Checkout();
        c.setPaymentMethod(method);
        c.setTotal(total);
        return c;
    }

    @Test
    void totalWithinLimitIsPaid() {
        Checkout result = service.processPayment(checkout("ewallet", 2_000_000));

        assertTrue(result.isPaymentSuccess());
        assertEquals(CheckoutStatus.PAID, result.getStatus());
        assertTrue(result.getPaymentReference().startsWith("PAY-"));
    }

    @Test
    void totalAboveLimitIsDeclined() {
        Checkout result = service.processPayment(checkout("EWALLET", 2_000_001));

        assertFalse(result.isPaymentSuccess());
        assertEquals(CheckoutStatus.PAYMENT_FAILED, result.getStatus());
        assertNull(result.getPaymentReference());
    }

    @Test
    void refundMarksCheckoutAsOrderFailed() {
        Checkout paid = service.processPayment(checkout("COD", 100_000));

        Checkout result = service.refundPayment(paid);

        assertTrue(result.isPaymentRefunded());
        assertEquals(CheckoutStatus.ORDER_FAILED, result.getStatus());
        assertTrue(result.getFailureReason().contains(result.getPaymentReference()));
    }

    @Test
    void unsupportedMethodIsDeclined() {
        Checkout result = service.processPayment(checkout("CRYPTO", 1_000));

        assertFalse(result.isPaymentSuccess());
        assertEquals("unsupported payment method: CRYPTO", result.getFailureReason());
    }
}
