package com.example.checkout.service;

import java.util.HashMap;
import java.util.Map;

import org.jboss.logging.Logger;

import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CartService {

    private static final Logger LOG = Logger.getLogger(CartService.class);
    static final int MAX_QTY_PER_ITEM = 10;

    @Inject
    ProductCatalog catalog;

    public Checkout validateCart(Checkout checkout) {
        String error = findError(checkout);
        if (error == null) {
            checkout.setCartValid(true);
            checkout.setStatus(CheckoutStatus.VALIDATED);
            LOG.infof("[cart] valid for customer %s (%d item)", checkout.getCustomerId(), checkout.getItems().size());
        } else {
            checkout.setCartValid(false);
            checkout.fail(CheckoutStatus.REJECTED_INVALID_CART, error);
            LOG.warnf("[cart] rejected: %s", error);
        }
        return checkout;
    }

    private String findError(Checkout checkout) {
        if (isBlank(checkout.getCustomerId())) {
            return "customerId is required";
        }
        if (checkout.getItems() == null || checkout.getItems().isEmpty()) {
            return "cart is empty";
        }
        if (isBlank(checkout.getPaymentMethod())) {
            return "paymentMethod is required";
        }
        if (!PaymentService.supports(checkout.getPaymentMethod())) {
            return "unsupported payment method: " + checkout.getPaymentMethod();
        }
        Map<String, Integer> quantityPerSku = new HashMap<>();
        for (CartItem item : checkout.getItems()) {
            if (isBlank(item.getSku()) || catalog.find(item.getSku()).isEmpty()) {
                return "unknown product: " + item.getSku();
            }
            if (item.getQuantity() < 1
                    || quantityPerSku.merge(item.getSku(), item.getQuantity(), Integer::sum) > MAX_QTY_PER_ITEM) {
                return "quantity for " + item.getSku() + " must be between 1 and " + MAX_QTY_PER_ITEM;
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
