package com.example.walkthrough;

import static com.example.walkthrough.WalkthroughSupport.applicant;
import static com.example.walkthrough.WalkthroughSupport.awaitCompleted;
import static com.example.walkthrough.WalkthroughSupport.awaitUserTask;
import static com.example.walkthrough.WalkthroughSupport.transition;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Langkah 3: User Task membuat proses berhenti (wait state) sampai ada manusia yang
 * menyelesaikan task-nya lewat API {@code /usertasks/instance}.
 */
@QuarkusTest
class Step03UserTaskTest {

    private String start() {
        return given().contentType(ContentType.JSON)
                .body(Map.of("applicant", applicant("Citra", 28, 8_000_000, 40_000_000, 680)))
                .when().post("/loan_approval")
                .then().statusCode(201)
                .extract().path("id");
    }

    @Test
    void processWaitsForOfficerThenApproves() {
        String id = start();

        // Proses masih aktif karena menunggu user task.
        given().get("/loan_approval/" + id).then().statusCode(200).body("id", equalTo(id));

        Map<String, Object> task = awaitUserTask(id);
        assertEquals("ReviewLoan", task.get("taskName"));
        String taskId = (String) task.get("id");

        transition(taskId, "claim", Map.of());
        transition(taskId, "complete", Map.of("approved", true, "note", "Dokumen lengkap"));

        var journal = awaitCompleted(id);
        assertEquals("APPROVED", journal.getString("variables.status"));
        assertEquals("Dokumen lengkap", journal.getString("variables.note"));
    }

    @Test
    void officerCanReject() {
        String id = start();
        String taskId = (String) awaitUserTask(id).get("id");

        transition(taskId, "claim", Map.of());
        transition(taskId, "complete", Map.of("approved", false, "note", "Penghasilan tidak terverifikasi"));

        assertEquals("REJECTED", awaitCompleted(id).getString("variables.status"));
    }

    @Test
    void userOutsideTheGroupCannotSeeTheTask() {
        String id = start();
        awaitUserTask(id);

        given().queryParam("user", "joko").queryParam("group", "marketing")
                .when().get("/usertasks/instance")
                .then().statusCode(200)
                .body("findAll { it.metadata.ProcessInstanceId == '" + id + "' }.size()", equalTo(0));
    }
}
