package com.example.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    ProductCatalog catalog;

    @InjectMocks
    InventoryService service;

    private static Checkout checkout() {
        Checkout c = new Checkout();
        c.setCustomerId("CUST-01");
        c.setItems(List.of(new CartItem("SKU-001", 2), new CartItem("SKU-001", 1), new CartItem("SKU-002", 1)));
        return c;
    }

    @Test
    void reserveSumsQuantityPerSkuAndMarksStockReserved() {
        when(catalog.tryReserve(Map.of("SKU-001", 3, "SKU-002", 1))).thenReturn(null);

        Checkout result = service.reserveStock(checkout());

        assertTrue(result.isStockReserved());
        assertEquals(CheckoutStatus.STOCK_RESERVED, result.getStatus());
    }

    @Test
    void reserveFailureRejectsCheckoutAsOutOfStock() {
        when(catalog.tryReserve(anyMap())).thenReturn("insufficient stock for SKU-002");

        Checkout result = service.reserveStock(checkout());

        assertFalse(result.isStockReserved());
        assertEquals(CheckoutStatus.REJECTED_OUT_OF_STOCK, result.getStatus());
        assertEquals("insufficient stock for SKU-002", result.getFailureReason());
    }

    @Test
    void releaseReturnsReservedStock() {
        Checkout checkout = checkout();
        checkout.setStockReserved(true);

        Checkout result = service.releaseStock(checkout);

        verify(catalog).release(Map.of("SKU-001", 3, "SKU-002", 1));
        assertFalse(result.isStockReserved());
    }

    @Test
    void releaseDoesNothingWhenStockWasNeverReserved() {
        service.releaseStock(checkout());

        verify(catalog, never()).release(anyMap());
    }
}
