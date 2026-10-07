package com.example.checkout;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.checkout.model.Checkout;
import com.example.checkout.repository.CheckoutRequestRepository;
import com.example.checkout.repository.OrderRepository;
import com.example.checkout.service.OrderService;
import com.example.checkout.service.ProductCatalog;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class CheckoutRestTest {

    @Inject
    ProductCatalog catalog;

    @Inject
    OrderRepository orderRepository;

    @Inject
    CheckoutRequestRepository checkoutRequestRepository;

    @BeforeEach
    void resetData() {
        catalog.reset();
        QuarkusTransaction.requiringNew().run(() -> {
            orderRepository.deleteAll();
            checkoutRequestRepository.deleteAll();
        });
    }

    @Test
    void postCheckout_successfulFlow_returnsCompletedCheckoutAndPersistsOrder() {
        String body = """
                {
                  "checkout": {
                    "customerId": "CUST-77",
                    "customerEmail": "siti@example.com",
                    "paymentMethod": "COD",
                    "items": [ { "sku": "SKU-002", "quantity": 2 } ]
                  }
                }
                """;

        String orderNumber = given().contentType(ContentType.JSON).accept(ContentType.JSON).body(body)
                .when().post("/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("id", notNullValue())
                .body("checkout.status", equalTo("COMPLETED"))
                .body("checkout.total", equalTo(500_000))
                .body("checkout.orderNumber", startsWith("ORD-"))
                .extract().path("checkout.orderNumber");

        given().when().get("/orders/" + orderNumber)
                .then().statusCode(200)
                .body("customerId", equalTo("CUST-77"))
                .body("total", equalTo(500_000))
                .body("items", hasSize(1))
                .body("items[0].sku", equalTo("SKU-002"))
                .body("items[0].quantity", equalTo(2))
                .body("items[0].lineTotal", equalTo(500_000));

        given().when().get("/orders").then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    void postCheckout_outOfStock_returnsRejectedStatus() {
        String body = """
                { "checkout": { "customerId": "CUST-78", "paymentMethod": "EWALLET",
                                "items": [ { "sku": "SKU-005", "quantity": 1 } ] } }
                """;

        given().contentType(ContentType.JSON).accept(ContentType.JSON).body(body)
                .when().post("/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("checkout.status", equalTo("REJECTED_OUT_OF_STOCK"));

        given().when().get("/orders").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void postCheckout_orderCreationFails_returnsOrderFailedAndRestoresStock() {
        OrderService failing = mock(OrderService.class);
        when(failing.createOrder(any(Checkout.class))).thenThrow(new IllegalStateException("database unavailable"));
        QuarkusMock.installMockForType(failing, OrderService.class);
        String body = """
                { "checkout": { "customerId": "CUST-80", "paymentMethod": "COD",
                                "items": [ { "sku": "SKU-002", "quantity": 2 } ] } }
                """;

        given().contentType(ContentType.JSON).accept(ContentType.JSON).body(body)
                .when().post("/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("checkout.status", equalTo("ORDER_FAILED"))
                .body("checkout.paymentRefunded", equalTo(true))
                .body("checkout.stockReserved", equalTo(false));

        given().when().get("/products").then().statusCode(200).body("[1].stock", equalTo(5));
        given().when().get("/orders").then().statusCode(200).body("$", hasSize(0));
    }

    @Test
    void postCheckout_sameRequestIdTwice_secondIsRejectedAsDuplicate() {
        String body = """
                { "checkout": { "requestId": "REQ-REST-1", "customerId": "CUST-79", "paymentMethod": "COD",
                                "items": [ { "sku": "SKU-001", "quantity": 1 } ] } }
                """;

        given().contentType(ContentType.JSON).accept(ContentType.JSON).body(body)
                .when().post("/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("checkout.status", equalTo("COMPLETED"));

        given().contentType(ContentType.JSON).accept(ContentType.JSON).body(body)
                .when().post("/checkout")
                .then()
                .statusCode(anyOf(is(200), is(201)))
                .body("checkout.status", equalTo("REJECTED_DUPLICATE_REQUEST"))
                .body("checkout.orderNumber", startsWith("ORD-"));

        given().when().get("/orders").then().statusCode(200).body("$", hasSize(1));
    }

    @Test
    void productsEndpointListsCatalog() {
        given().when().get("/products")
                .then().statusCode(200)
                .body("$", hasSize(5))
                .body("[0].sku", equalTo("SKU-001"));
    }
}
