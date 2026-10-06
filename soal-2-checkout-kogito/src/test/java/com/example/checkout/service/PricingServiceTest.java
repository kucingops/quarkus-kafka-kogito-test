package com.example.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;
import com.example.checkout.service.ProductCatalog.Product;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    ProductCatalog catalog;

    @InjectMocks
    PricingService service;

    private Checkout checkout(String voucher, long price, int quantity) {
        when(catalog.find("SKU-X")).thenReturn(Optional.of(new Product("SKU-X", "Produk X", price, 99)));
        Checkout c = new Checkout();
        c.setVoucherCode(voucher);
        c.setItems(List.of(new CartItem("SKU-X", quantity)));
        return c;
    }

    @Test
    void smallOrderPaysShippingFee() {
        Checkout result = service.calculateTotal(checkout(null, 100_000, 2));

        assertEquals(200_000, result.getSubtotal());
        assertEquals(20_000, result.getShippingFee());
        assertEquals(0, result.getDiscount());
        assertEquals(220_000, result.getTotal());
        assertEquals(CheckoutStatus.PRICED, result.getStatus());
        assertEquals("Produk X", result.getItems().get(0).getProductName());
        assertEquals(200_000, result.getItems().get(0).getLineTotal());
    }

    @Test
    void largeOrderGetsFreeShipping() {
        Checkout result = service.calculateTotal(checkout(null, 250_000, 2));

        assertEquals(0, result.getShippingFee());
        assertEquals(500_000, result.getTotal());
    }

    @Test
    void voucherGivesTenPercentDiscount() {
        Checkout result = service.calculateTotal(checkout("hemat10", 100_000, 2));

        assertEquals(20_000, result.getDiscount());
        assertEquals(200_000, result.getTotal());
    }

    @Test
    void voucherDiscountIsCapped() {
        Checkout result = service.calculateTotal(checkout("HEMAT10", 1_000_000, 1));

        assertEquals(50_000, result.getDiscount());
        assertEquals(950_000, result.getTotal());
    }
}
