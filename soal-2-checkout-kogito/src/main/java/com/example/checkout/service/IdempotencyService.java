package com.example.checkout.service;

import java.time.Instant;

import org.jboss.logging.Logger;

import com.example.checkout.entity.CheckoutRequestEntity;
import com.example.checkout.entity.OrderEntity;
import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;
import com.example.checkout.repository.CheckoutRequestRepository;
import com.example.checkout.repository.OrderRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class IdempotencyService {

    private static final Logger LOG = Logger.getLogger(IdempotencyService.class);

    @Inject
    CheckoutRequestRepository checkoutRequestRepository;

    @Inject
    OrderRepository orderRepository;

    static String normalize(String requestId) {
        return requestId == null || requestId.isBlank() ? null : requestId.trim();
    }

    public Checkout checkDuplicate(Checkout checkout) {
        String requestId = normalize(checkout.getRequestId());
        String customerId = checkout.getCustomerId();
        if (requestId == null) {
            return checkout;
        }
        if (claim(customerId, requestId)) {
            LOG.infof("[idempotency] claimed request %s for customer %s", requestId, customerId);
            return checkout;
        }
        checkout.setDuplicateRequest(true);
        checkout.fail(CheckoutStatus.REJECTED_DUPLICATE_REQUEST, "duplicate request: " + requestId);
        checkout.setOrderNumber(originalOrderNumber(customerId, requestId));
        LOG.warnf("[idempotency] duplicate request %s for customer %s", requestId, customerId);
        return checkout;
    }

    private boolean claim(String customerId, String requestId) {
        if (exists(customerId, requestId)) {
            return false;
        }
        try {
            QuarkusTransaction.requiringNew().run(() -> {
                CheckoutRequestEntity request = new CheckoutRequestEntity();
                request.requestId = requestId;
                request.customerId = customerId;
                request.createdAt = Instant.now();
                checkoutRequestRepository.persistAndFlush(request);
            });
            return true;
        } catch (RuntimeException e) {
            if (exists(customerId, requestId)) {
                return false;
            }
            throw e;
        }
    }

    private boolean exists(String customerId, String requestId) {
        return QuarkusTransaction.requiringNew().call(() -> checkoutRequestRepository.existsByRequest(customerId, requestId));
    }

    private String originalOrderNumber(String customerId, String requestId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            OrderEntity order = orderRepository.findByRequest(customerId, requestId);
            return order == null ? null : order.orderNumber;
        });
    }
}
