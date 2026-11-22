package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 6: Rule Unit DRL. Setiap {@code query} di {@code LoanRules.drl} menjadi endpoint
 * REST ({@code FindAlerts} -> {@code POST /find-alerts}). Body request adalah isi data source
 * rule unit ({@code applicants}), dan respons adalah hasil query setelah semua rule dijalankan.
 */
@QuarkusTest
class Step06RulesTest {

    private static Map<String, Object> unit(Map<?, ?>... applicants) {
        return Map.of("applicants", List.of(applicants));
    }

    @Test
    void healthyApplicantHasNoAlerts() {
        given().contentType(ContentType.JSON)
                .body(unit(applicant("Indra", 30, 10_000_000, 20_000_000, 720)))
                .when().post("/find-alerts")
                .then().statusCode(200)
                .body("$", empty());
    }

    @Test
    void eachRuleFiresIndependently() {
        given().contentType(ContentType.JSON)
                .body(unit(
                        applicant("Joko", 30, 10_000_000, 20_000_000, 550),   // skor rendah
                        applicant("Kiki", 19, 10_000_000, 20_000_000, 720),   // di bawah umur
                        applicant("Lina", 30, 10_000_000, 50_000_000, 720)))  // cicilan berat
                .when().post("/find-alerts")
                .then().statusCode(200)
                .body("$", hasSize(3))
                .body("applicant", containsInAnyOrder("Joko", "Kiki", "Lina"))
                .body("message", containsInAnyOrder(
                        "Skor kredit di bawah 600",
                        "Usia di bawah 21 tahun",
                        "Cicilan melebihi 30% penghasilan"));
    }

    @Test
    void forwardChainingAddsCriticalAlert() {
        // Tiga rule biasa cocok untuk Mira, lalu rule "Banyak peringatan" ikut terpicu.
        var applicants = unit(
                applicant("Mira", 19, 5_000_000, 50_000_000, 500),
                applicant("Nanda", 30, 10_000_000, 20_000_000, 720));

        given().contentType(ContentType.JSON).body(applicants)
                .when().post("/find-alerts")
                .then().statusCode(200)
                .body("$", hasSize(4));

        given().contentType(ContentType.JSON).body(applicants)
                .when().post("/find-critical-alerts")
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].applicant", equalTo("Mira"))
                .body("[0].message", equalTo("Ada 3 peringatan, wajib review manual"));
    }
}
