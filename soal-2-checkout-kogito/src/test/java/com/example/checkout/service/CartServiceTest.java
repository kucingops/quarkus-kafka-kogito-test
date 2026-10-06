package com.example.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
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
class CartServiceTest {

    @Mock
    ProductCatalog catalog;

    @InjectMocks
    CartService service;

    private static Checkout checkout(CartItem... items) {
        Checkout c = new Checkout();
        c.setCustomerId("CUST-01");
        c.setPaymentMethod("BANK_TRANSFER");
        c.setItems(List.of(items));
        return c;
    }

    @Test
    void knownProductWithValidQuantityIsAccepted() {
        when(catalog.find("SKU-001")).thenReturn(Optional.of(new Product("SKU-001", "Kaos Polos", 75_000, 10)));

        Checkout result = service.validateCart(checkout(new CartItem("SKU-001", 2)));

        assertTrue(result.isCartValid());
        assertEquals(CheckoutStatus.VALIDATED, result.getStatus());
    }

    @Test
    void emptyCartIsRejected() {
        Checkout result = service.validateCart(checkout());

        assertFalse(result.isCartValid());
        assertEquals(CheckoutStatus.REJECTED_INVALID_CART, result.getStatus());
        assertEquals("cart is empty", result.getFailureReason());
        verifyNoInteractions(catalog);
    }

    @Test
    void unknownProductIsRejected() {
        when(catalog.find("SKU-999")).thenReturn(Optional.empty());

        Checkout result = service.validateCart(checkout(new CartItem("SKU-999", 1)));

        assertFalse(result.isCartValid());
        assertEquals("unknown product: SKU-999", result.getFailureReason());
    }

    @Test
    void quantityAboveLimitIsRejected() {
        when(catalog.find("SKU-001")).thenReturn(Optional.of(new Product("SKU-001", "Kaos Polos", 75_000, 10)));

        Checkout result = service.validateCart(checkout(new CartItem("SKU-001", 11)));

        assertFalse(result.isCartValid());
        assertEquals(CheckoutStatus.REJECTED_INVALID_CART, result.getStatus());
    }
}
