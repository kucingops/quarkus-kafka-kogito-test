package com.example.checkout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.checkout.ConcurrentRunner;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class ProductCatalogTest {

    private static final int THREADS = 32;

    @Inject
    ProductCatalog catalog;

    @BeforeEach
    void resetData() {
        catalog.reset();
    }

    @Test
    void reserveFailsWithoutPartialDeduction() {
        String error = catalog.tryReserve(Map.of("SKU-001", 1, "SKU-005", 1));

        assertNotNull(error);
        assertEquals(10, catalog.stockOf("SKU-001"));
    }

    @Test
    void concurrentReservationsNeverExceedStock() throws Exception {
        List<String> errors = ConcurrentRunner.run(THREADS, () -> catalog.tryReserve(Map.of("SKU-003", 1)));

        assertEquals(3, errors.stream().filter(Objects::isNull).count());
        assertEquals(0, catalog.stockOf("SKU-003"));
    }

    @Test
    void concurrentMultiSkuReservationsAreAllOrNothing() throws Exception {
        List<String> errors = ConcurrentRunner.run(THREADS,
                () -> catalog.tryReserve(Map.of("SKU-001", 1, "SKU-004", 1)));

        assertEquals(2, errors.stream().filter(Objects::isNull).count());
        assertEquals(8, catalog.stockOf("SKU-001"));
        assertEquals(0, catalog.stockOf("SKU-004"));
    }

    @Test
    void concurrentReserveAndReleaseKeepsStockConsistent() throws Exception {
        List<String> errors = ConcurrentRunner.run(THREADS, () -> {
            String error = null;
            for (int i = 0; i < 50; i++) {
                error = catalog.tryReserve(Map.of("SKU-001", 1));
                if (error == null) {
                    catalog.release(Map.of("SKU-001", 1));
                }
            }
            return error;
        });

        assertEquals(THREADS, errors.size());
        assertEquals(10, catalog.stockOf("SKU-001"));
    }

    @Test
    void releaseRestoresStock() {
        assertNull(catalog.tryReserve(Map.of("SKU-002", 2)));
        catalog.release(Map.of("SKU-002", 2));

        assertEquals(5, catalog.stockOf("SKU-002"));
    }
}
