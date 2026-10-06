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
public class InventoryService {

    private static final Logger LOG = Logger.getLogger(InventoryService.class);

    @Inject
    ProductCatalog catalog;

    public Checkout reserveStock(Checkout checkout) {
        Map<String, Integer> quantities = totalQuantityPerSku(checkout);
        String error = catalog.tryReserve(quantities);

        if (error != null) {
            checkout.setStockReserved(false);
            checkout.fail(CheckoutStatus.REJECTED_OUT_OF_STOCK, error);
            LOG.warnf("[inventory] %s", error);
            return checkout;
        }
        checkout.setStockReserved(true);
        checkout.setStatus(CheckoutStatus.STOCK_RESERVED);
        LOG.infof("[inventory] reserved %s", quantities);
        return checkout;
    }

    public Checkout releaseStock(Checkout checkout) {
        if (checkout.isStockReserved()) {
            catalog.release(totalQuantityPerSku(checkout));
            checkout.setStockReserved(false);
            LOG.infof("[inventory] released stock for customer %s", checkout.getCustomerId());
        }
        checkout.setStatus(CheckoutStatus.PAYMENT_FAILED);
        return checkout;
    }

    private static Map<String, Integer> totalQuantityPerSku(Checkout checkout) {
        Map<String, Integer> result = new HashMap<>();
        for (CartItem item : checkout.getItems()) {
            result.merge(item.getSku(), item.getQuantity(), Integer::sum);
        }
        return result;
    }
}
