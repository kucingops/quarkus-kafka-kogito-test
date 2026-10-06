package com.example.checkout.service;

import java.time.Instant;

import org.jboss.logging.Logger;

import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;
import com.example.checkout.persistence.CheckoutRequestEntity;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IdempotencyService {

    private static final Logger LOG = Logger.getLogger(IdempotencyService.class);

    public Checkout checkDuplicate(Checkout checkout) {
        String requestId = checkout.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            return checkout;
        }
        if (claim(requestId.trim(), checkout.getCustomerId())) {
            LOG.infof("[idempotency] claimed request %s", requestId);
            return checkout;
        }
        checkout.setDuplicateRequest(true);
        checkout.fail(CheckoutStatus.REJECTED_DUPLICATE_REQUEST, "duplicate request: " + requestId);
        LOG.warnf("[idempotency] duplicate request %s", requestId);
        return checkout;
    }

    private boolean claim(String requestId, String customerId) {
        if (exists(requestId)) {
            return false;
        }
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                CheckoutRequestEntity request = new CheckoutRequestEntity();
                request.requestId = requestId;
                request.customerId = customerId;
                request.createdAt = Instant.now();
                request.persistAndFlush();
            });
            return true;
        } catch (RuntimeException e) {
            if (exists(requestId)) {
                return false;
            }
            throw e;
        }
    }

    private boolean exists(String requestId) {
        return QuarkusTransaction.requiringNew().call(() -> CheckoutRequestEntity.existsByRequestId(requestId));
    }
}
