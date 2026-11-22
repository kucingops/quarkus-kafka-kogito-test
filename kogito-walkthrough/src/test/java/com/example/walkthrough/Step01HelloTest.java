package com.example.walkthrough;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 1: proses paling sederhana. Kogito men-generate endpoint {@code POST /hello}
 * dari {@code step01-hello.bpmn}, lengkap dengan model input/output dari process variable.
 */
@QuarkusTest
class Step01HelloTest {

    @Test
    void startingTheProcessRunsTheScriptTask() {
        String id = given().contentType(ContentType.JSON)
                .body("{\"name\": \"Hilmi\"}")
                .when().post("/hello")
                .then().statusCode(201)
                .body("id", notNullValue())
                .body("greeting", equalTo("Halo, Hilmi! Selamat datang di Kogito."))
                .extract().path("id");

        // Proses tanpa wait state langsung selesai, jadi Kogito sudah menghapusnya...
        given().when().get("/hello/" + id).then().statusCode(404);

        // ...tapi event listener kita sempat mencatatnya.
        given().when().get("/journal/" + id)
                .then().statusCode(200)
                .body("completed", equalTo(true))
                .body("nodes", hasItems("Mulai", "Susun Sapaan", "Selesai"));
    }

    @Test
    void emptyNameFallsBackToDunia() {
        given().contentType(ContentType.JSON)
                .body("{}")
                .when().post("/hello")
                .then().statusCode(201)
                .body("greeting", equalTo("Halo, Dunia! Selamat datang di Kogito."));
    }
}
