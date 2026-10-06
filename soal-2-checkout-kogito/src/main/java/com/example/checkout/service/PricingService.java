package com.example.checkout.service;

import java.util.Locale;

import org.jboss.logging.Logger;

import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;
import com.example.checkout.service.ProductCatalog.Product;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PricingService {

    private static final Logger LOG = Logger.getLogger(PricingService.class);

    static final long SHIPPING_FEE = 20_000;
    static final long FREE_SHIPPING_MIN = 500_000;
    static final String VOUCHER_CODE = "HEMAT10";
    static final long VOUCHER_MAX_DISCOUNT = 50_000;

    @Inject
    ProductCatalog catalog;

    public Checkout calculateTotal(Checkout checkout) {
        long subtotal = 0;
        for (CartItem item : checkout.getItems()) {
            Product product = catalog.find(item.getSku()).orElseThrow();
            item.setProductName(product.name());
            item.setUnitPrice(product.price());
            item.setLineTotal(product.price() * item.getQuantity());
            subtotal += item.getLineTotal();
        }

        long shipping = subtotal >= FREE_SHIPPING_MIN ? 0 : SHIPPING_FEE;
        long discount = 0;
        if (checkout.getVoucherCode() != null
                && VOUCHER_CODE.equals(checkout.getVoucherCode().trim().toUpperCase(Locale.ROOT))) {
            discount = Math.min(subtotal / 10, VOUCHER_MAX_DISCOUNT);
        }

        checkout.setSubtotal(subtotal);
        checkout.setShippingFee(shipping);
        checkout.setDiscount(discount);
        checkout.setTotal(subtotal + shipping - discount);
        checkout.setStatus(CheckoutStatus.PRICED);
        LOG.infof("[pricing] subtotal=%d shipping=%d discount=%d total=%d",
                subtotal, shipping, discount, checkout.getTotal());
        return checkout;
    }
}
