package com.example.checkout.service;

import org.jboss.logging.Logger;

import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationService.class);

    public Checkout sendOrderConfirmation(Checkout checkout) {
        String to = checkout.getCustomerEmail() != null ? checkout.getCustomerEmail() : checkout.getCustomerId();
        LOG.infof("[notification] to=%s: Pesanan %s berhasil dibayar. Total Rp%,d.",
                to, checkout.getOrderNumber(), checkout.getTotal());

        checkout.setNotificationSent(true);
        checkout.setStatus(CheckoutStatus.COMPLETED);
        return checkout;
    }
}
