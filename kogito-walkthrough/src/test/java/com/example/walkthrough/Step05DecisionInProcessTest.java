package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static com.example.walkthrough.WalkthroughSupport.awaitCompleted;
import static com.example.walkthrough.WalkthroughSupport.awaitUserTask;
import static com.example.walkthrough.WalkthroughSupport.transition;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 5: Business Rule Task memanggil DMN dari Langkah 4 di tengah proses, dan
 * Call Activity memakai ulang proses Langkah 3 sebagai subproses.
 */
@QuarkusTest
class Step05DecisionInProcessTest {

    private static String start(Map<String, Object> applicant) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("applicant", applicant))
                .when().post("/loan_decision")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void approvedByDmnCompletesImmediately() {
        String id = start(applicant("Fajar", 30, 10_000_000, 50_000_000, 750));

        var journal = awaitCompleted(id);
        assertEquals("APPROVED", journal.getString("variables.eligibility"));
        assertEquals("LOW", journal.getString("variables.riskCategory"));
        assertEquals("APPROVED", journal.getString("variables.status"));
    }

    @Test
    void rejectedByDmn() {
        String id = start(applicant("Gita", 30, 10_000_000, 50_000_000, 500));
        assertEquals("REJECTED", awaitCompleted(id).getString("variables.status"));
    }

    @Test
    void reviewStartsTheApprovalSubprocess() {
        // Di atas plafon (10 jt x 6 = 60 jt) -> REVIEW -> Call Activity ke loan_approval.
        String id = start(applicant("Hana", 30, 10_000_000, 80_000_000, 650));

        given().get("/loan_decision/" + id).then().statusCode(200)
                .body("eligibility", equalTo("REVIEW"))
                .body("riskCategory", equalTo("MEDIUM"));

        // User task-nya milik process instance anak (loan_approval), bukan induknya.
        String childId = given().get("/loan_approval").then().statusCode(200)
                .extract().jsonPath().getList("findAll { it.applicant.name == 'Hana' }.id", String.class)
                .get(0);
        String taskId = (String) awaitUserTask(childId).get("id");
        transition(taskId, "claim", Map.of());
        transition(taskId, "complete", Map.of("approved", true, "note", "Disetujui komite"));

        var parent = awaitCompleted(id);
        assertEquals("APPROVED", parent.getString("variables.status"));
        assertTrue(parent.getList("nodes", String.class).contains("Review Manual (Langkah 3)"));
    }
}
