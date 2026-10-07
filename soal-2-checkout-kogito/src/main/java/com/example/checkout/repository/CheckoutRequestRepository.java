package com.example.checkout.repository;

import com.example.checkout.entity.CheckoutRequestEntity;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CheckoutRequestRepository implements PanacheRepository<CheckoutRequestEntity> {

    public boolean existsByRequest(String customerId, String requestId) {
        return count("customerId = ?1 and requestId = ?2", customerId, requestId) > 0;
    }
}
