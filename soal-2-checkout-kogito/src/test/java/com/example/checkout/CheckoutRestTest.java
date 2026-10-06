package com.example.checkout;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.checkout.persistence.CheckoutRequestEntity;
import com.example.checkout.persistence.OrderEntity;
import com.example.checkout.service.ProductCatalog;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

@QuarkusTest
class CheckoutRestTest {

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
                .body("total", equalTo(500_000));

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
                .body("checkout.status", equalTo("REJECTED_DUPLICATE_REQUEST"));

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
