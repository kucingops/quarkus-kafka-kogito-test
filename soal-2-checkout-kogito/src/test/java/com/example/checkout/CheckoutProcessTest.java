package com.example.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.kogito.Model;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;

import com.example.checkout.model.CartItem;
import com.example.checkout.model.Checkout;
import com.example.checkout.model.CheckoutStatus;
import com.example.checkout.persistence.CheckoutRequestEntity;
import com.example.checkout.persistence.OrderEntity;
import com.example.checkout.service.ProductCatalog;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@QuarkusTest
class CheckoutProcessTest {

    @Inject
    @Named("checkout")
    Process<? extends Model> checkoutProcess;

    @Inject
    ProductCatalog catalog;

    @BeforeEach
    void resetData() {
        catalog.reset();
        QuarkusTransaction.requiringNew().run(() -> {
            OrderEntity.deleteAll();
            CheckoutRequestEntity.deleteAll();
        });
    }

    private static Checkout request(String paymentMethod, CartItem... items) {
        Checkout c = new Checkout();
        c.setCustomerId("CUST-01");
        c.setCustomerEmail("budi@example.com");
        c.setPaymentMethod(paymentMethod);
        c.setItems(new ArrayList<>(List.of(items)));
        return c;
    }

    private Checkout run(Checkout input) {
        Model model = checkoutProcess.createModel();
        model.fromMap(Map.of("checkout", input));

        ProcessInstance<?> instance = checkoutProcess.createInstance(model);
        instance.start();

        assertEquals(org.kie.api.runtime.process.ProcessInstance.STATE_COMPLETED, instance.status(),
                "process instance harus mencapai salah satu End Event");
        Model variables = (Model) instance.variables();
        return (Checkout) variables.toMap().get("checkout");
    }

    private long orderCount() {
        return QuarkusTransaction.requiringNew().call(() -> OrderEntity.count());
    }

    private static long countByStatus(List<Checkout> results, CheckoutStatus status) {
        return results.stream().filter(c -> c.getStatus() == status).count();
    }

    @Test
    void happyPath_validCart_stockAvailable_paymentSuccess_orderCreated() {
        Checkout result = run(request("BANK_TRANSFER",
                new CartItem("SKU-001", 2),
                new CartItem("SKU-002", 1)));

        assertEquals(CheckoutStatus.COMPLETED, result.getStatus());
        assertTrue(result.isCartValid());
        assertTrue(result.isStockReserved());
        assertTrue(result.isPaymentSuccess());
        assertTrue(result.isNotificationSent());
        assertNull(result.getFailureReason());

        assertEquals(400_000, result.getSubtotal());
        assertEquals(20_000, result.getShippingFee());
        assertEquals(420_000, result.getTotal());
        assertEquals("Kaos Polos", result.getItems().get(0).getProductName());

        assertEquals(8, catalog.stockOf("SKU-001"));
        assertEquals(4, catalog.stockOf("SKU-002"));
        assertNotNull(result.getOrderNumber());
        assertNotNull(result.getPaymentReference());
        assertEquals(1, orderCount());
    }

    @Test
    void happyPath_withVoucherAndFreeShipping() {
        Checkout input = request("CREDIT_CARD", new CartItem("SKU-003", 1));
        input.setVoucherCode("hemat10");

        Checkout result = run(input);

        assertEquals(CheckoutStatus.COMPLETED, result.getStatus());
        assertEquals(0, result.getShippingFee());
        assertEquals(50_000, result.getDiscount());
        assertEquals(600_000, result.getTotal());
    }

    @Test
    void invalidCart_endsAtRejectedEvent_withoutTouchingStock() {
        Checkout result = run(request("BANK_TRANSFER"));

        assertEquals(CheckoutStatus.REJECTED_INVALID_CART, result.getStatus());
        assertEquals("cart is empty", result.getFailureReason());
        assertFalse(result.isStockReserved());
        assertEquals(0, orderCount());
    }

    @Test
    void unknownProduct_isRejectedAsInvalidCart() {
        Checkout result = run(request("BANK_TRANSFER", new CartItem("SKU-404", 1)));

        assertEquals(CheckoutStatus.REJECTED_INVALID_CART, result.getStatus());
        assertEquals("unknown product: SKU-404", result.getFailureReason());
    }

    @Test
    void outOfStock_endsAtOutOfStockEvent_andNoPartialReservation() {
        Checkout result = run(request("BANK_TRANSFER",
                new CartItem("SKU-001", 1),
                new CartItem("SKU-005", 1)));

        assertEquals(CheckoutStatus.REJECTED_OUT_OF_STOCK, result.getStatus());
        assertTrue(result.getFailureReason().contains("SKU-005"));
        assertEquals(10, catalog.stockOf("SKU-001"));
        assertEquals(0, result.getTotal());
        assertEquals(0, orderCount());
    }

    @Test
    void paymentFailed_releasesReservedStock_andNoOrderCreated() {
        Checkout result = run(request("EWALLET", new CartItem("SKU-004", 1)));

        assertEquals(CheckoutStatus.PAYMENT_FAILED, result.getStatus());
        assertTrue(result.getFailureReason().startsWith("payment declined"));
        assertFalse(result.isPaymentSuccess());
        assertFalse(result.isStockReserved());
        assertEquals(2, catalog.stockOf("SKU-004"));
        assertEquals(0, orderCount());
    }

    @Test
    void concurrentCheckouts_neverOversellStock() throws Exception {
        int buyers = 20;
        int stock = catalog.stockOf("SKU-004");

        List<Checkout> results = ConcurrentRunner.run(buyers,
                () -> run(request("BANK_TRANSFER", new CartItem("SKU-004", 1))));

        assertEquals(stock, countByStatus(results, CheckoutStatus.COMPLETED));
        assertEquals(buyers - stock, countByStatus(results, CheckoutStatus.REJECTED_OUT_OF_STOCK));
        assertEquals(0, catalog.stockOf("SKU-004"));
        assertEquals(stock, orderCount());
        assertEquals(stock, results.stream().map(Checkout::getOrderNumber).filter(Objects::nonNull).distinct().count());
    }

    @Test
    void duplicateRequest_isRejectedWithoutSecondOrderOrStockDeduction() {
        Checkout first = request("BANK_TRANSFER", new CartItem("SKU-001", 2));
        first.setRequestId("REQ-001");
        Checkout second = request("BANK_TRANSFER", new CartItem("SKU-001", 2));
        second.setRequestId("REQ-001");

        assertEquals(CheckoutStatus.COMPLETED, run(first).getStatus());
        Checkout result = run(second);

        assertEquals(CheckoutStatus.REJECTED_DUPLICATE_REQUEST, result.getStatus());
        assertEquals("duplicate request: REQ-001", result.getFailureReason());
        assertTrue(result.isDuplicateRequest());
        assertFalse(result.isStockReserved());
        assertEquals(8, catalog.stockOf("SKU-001"));
        assertEquals(1, orderCount());
    }

    @Test
    void differentRequestIds_areProcessedIndependently() {
        Checkout first = request("BANK_TRANSFER", new CartItem("SKU-001", 1));
        first.setRequestId("REQ-A");
        Checkout second = request("BANK_TRANSFER", new CartItem("SKU-001", 1));
        second.setRequestId("REQ-B");

        assertEquals(CheckoutStatus.COMPLETED, run(first).getStatus());
        assertEquals(CheckoutStatus.COMPLETED, run(second).getStatus());
        assertEquals(2, orderCount());
    }

    @Test
    void concurrentDuplicateRequests_createExactlyOneOrder() throws Exception {
        int submissions = 20;

        List<Checkout> results = ConcurrentRunner.run(submissions, () -> {
            Checkout c = request("BANK_TRANSFER", new CartItem("SKU-001", 1));
            c.setRequestId("REQ-DOUBLE-CLICK");
            return run(c);
        });

        assertEquals(1, countByStatus(results, CheckoutStatus.COMPLETED));
        assertEquals(submissions - 1, countByStatus(results, CheckoutStatus.REJECTED_DUPLICATE_REQUEST));
        assertEquals(9, catalog.stockOf("SKU-001"));
        assertEquals(1, orderCount());
    }
}
