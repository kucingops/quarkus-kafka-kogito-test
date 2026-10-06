package com.example.checkout.service;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;

import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    static final Map<String, Long> LIMITS = Map.of(
            "BANK_TRANSFER", 50_000_000L,
            "CREDIT_CARD", 25_000_000L,
            "EWALLET", 2_000_000L,
            "COD", 5_000_000L);

    public Checkout processPayment(Checkout checkout) {
        String method = checkout.getPaymentMethod().trim().toUpperCase(Locale.ROOT);
        Long limit = LIMITS.get(method);

        if (limit == null) {
            return declined(checkout, "unsupported payment method: " + checkout.getPaymentMethod());
        }
        if (checkout.getTotal() > limit) {
            return declined(checkout, "payment declined: total " + checkout.getTotal()
                    + " exceeds " + method + " limit " + limit);
        }

        checkout.setPaymentSuccess(true);
        checkout.setPaymentReference("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        checkout.setStatus(CheckoutStatus.PAID);
        LOG.infof("[payment] %s paid %d via %s", checkout.getPaymentReference(), checkout.getTotal(), method);
        return checkout;
    }

    private Checkout declined(Checkout checkout, String reason) {
        checkout.setPaymentSuccess(false);
        checkout.fail(CheckoutStatus.PAYMENT_FAILED, reason);
        LOG.warnf("[payment] %s", reason);
        return checkout;
    }
}
