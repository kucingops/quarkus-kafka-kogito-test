package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static com.example.walkthrough.WalkthroughSupport.awaitCompleted;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 7: Event-Based Gateway menunggu salah satu dari dua event: signal
 * {@code documentsUploaded} atau timer. Mana yang datang duluan menentukan cabangnya.
 */
@QuarkusTest
class Step07EventsTest {

    private static String start(String timeout) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("applicant", applicant("Oki", 30, 10_000_000, 20_000_000, 720), "timeout", timeout))
                .when().post("/loan_documents")
                .then().statusCode(201)
                .body("status", equalTo("WAITING_DOCUMENTS"))
                .extract().path("id");
    }

    @Test
    void signalArrivesBeforeTimer() {
        String id = start("PT1H");

        // Endpoint signal di-generate dari nama signal di BPMN. Karena tipe datanya String,
        // body dibaca apa adanya (tanpa tanda kutip JSON).
        given().contentType(ContentType.JSON)
                .body("KTP, NPWP, slip gaji")
                .when().post("/loan_documents/" + id + "/documentsUploaded")
                .then().statusCode(200);

        var journal = awaitCompleted(id);
        assertEquals("DOCUMENTS_RECEIVED", journal.getString("variables.status"));
        assertEquals("KTP, NPWP, slip gaji", journal.getString("variables.documents"));
    }

    @Test
    void timerFiresWhenNoSignalArrives() {
        String id = start("PT1S");

        var journal = awaitCompleted(id);
        assertEquals("EXPIRED", journal.getString("variables.status"));
    }
}
