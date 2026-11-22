package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 4: DMN berdiri sendiri. Kogito men-generate {@code POST /LoanEligibility} dari
 * {@code LoanEligibility.dmn}; body-nya adalah semua Input Data, hasilnya semua Decision.
 */
@QuarkusTest
class Step04DmnTest {

    @ParameterizedTest(name = "umur {0}, skor {1}, pinjaman {2} -> {3}/{4}")
    @CsvSource({
            "30, 750, 100000000, LOW,    APPROVED", // plafon 10 jt x 12 = 120 jt
            "30, 750, 150000000, LOW,    REVIEW",   // di atas plafon
            "30, 650, 50000000,  MEDIUM, APPROVED", // plafon 10 jt x 6 = 60 jt
            "30, 550, 1000000,   HIGH,   REJECTED", // risiko tinggi
            "19, 800, 1000000,   LOW,    REJECTED", // di bawah 21 tahun (rule pertama menang)
    })
    void decisionTableAndLiteralExpression(int age, int score, long amount, String risk, String eligibility) {
        given().contentType(ContentType.JSON)
                .body(Map.of(
                        "Applicant", applicant("Dewi", age, 10_000_000, amount, score),
                        "Requested Amount", amount))
                .when().post("/LoanEligibility")
                .then().statusCode(200)
                .body("'Risk Category'", equalTo(risk))
                .body("Eligibility", equalTo(eligibility));
    }

    @Test
    void maxLoanIsComputedByFeel() {
        given().contentType(ContentType.JSON)
                .body(Map.of(
                        "Applicant", applicant("Eka", 35, 5_000_000, 1, 720),
                        "Requested Amount", 1))
                .when().post("/LoanEligibility")
                .then().statusCode(200)
                .body("'Max Loan'", equalTo(60_000_000));
    }
}
