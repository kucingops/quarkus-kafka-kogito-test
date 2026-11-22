package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static com.example.walkthrough.WalkthroughSupport.awaitCompleted;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.walkthrough.service.FundService;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;

/**
 * Langkah 8: Boundary Error Event menangkap exception dari Service Task, lalu
 * Intermediate Throw Compensation Event menjalankan compensation handler dari
 * semua task yang sudah selesai (di sini: melepas reservasi dana).
 */
@QuarkusTest
class Step08CompensationTest {

    @Inject
    FundService fundService;

    @BeforeEach
    void clear() {
        fundService.clearJournal();
    }

    private static String start(String name, long amount) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("applicant", applicant(name, 30, 100_000_000, amount, 750)))
                .when().post("/loan_disbursement")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void happyPathDoesNotCompensate() {
        String id = start("Putri", 50_000_000);

        assertEquals("DISBURSED", awaitCompleted(id).getString("variables.status"));
        assertEquals(List.of("RESERVE Putri 50000000", "TRANSFER Putri 50000000"), fundService.journal());
    }

    @Test
    void failedTransferTriggersCompensation() {
        String id = start("Raka", 150_000_000);

        var journal = awaitCompleted(id);
        assertEquals("DISBURSEMENT_FAILED", journal.getString("variables.status"));
        assertEquals(List.of(
                "RESERVE Raka 150000000",
                "TRANSFER_FAILED Raka",
                "RELEASE Raka 150000000"), fundService.journal());
    }
}
