package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 2: Service Task memanggil {@code ScoringService.score}, lalu Exclusive Gateway
 * memilih satu dari tiga cabang berdasarkan process variable {@code score}.
 */
@QuarkusTest
class Step02RoutingTest {

    @ParameterizedTest(name = "skor kredit {0}, pinjaman {1} -> {2}")
    @CsvSource({
            "750, 30000000,  AUTO_APPROVED, 750",
            "650, 30000000,  MANUAL_REVIEW, 650",
            "750, 200000000, MANUAL_REVIEW, 650", // cicilan > 30% penghasilan: skor -100
            "450, 30000000,  REJECTED,      450",
    })
    void gatewayPicksBranchFromScore(int creditScore, long amount, String expectedRoute, int expectedScore) {
        given().contentType(ContentType.JSON)
                .body(Map.of("applicant", applicant("Budi", 30, 10_000_000, amount, creditScore)))
                .when().post("/loan_routing")
                .then().statusCode(201)
                .body("score", equalTo(expectedScore))
                .body("route", equalTo(expectedRoute));
    }
}
