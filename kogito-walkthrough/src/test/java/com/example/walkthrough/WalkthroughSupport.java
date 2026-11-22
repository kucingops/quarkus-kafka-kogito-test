package com.example.walkthrough;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

/** Helper kecil yang dipakai bersama oleh test tiap langkah. */
final class WalkthroughSupport {

    static final String OFFICER = "ani";
    static final String OFFICER_GROUP = "credit-officers";

    private WalkthroughSupport() {
    }

    static Map<String, Object> applicant(String name, int age, long income, long amount, int creditScore) {
        return Map.of(
                "name", name,
                "age", age,
                "monthlyIncome", income,
                "requestedAmount", amount,
                "creditScore", creditScore);
    }

    /** Menunggu sampai {@code ProcessJournal} mencatat proses ini selesai, lalu mengembalikan catatannya. */
    static JsonPath awaitCompleted(String processInstanceId) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100)).until(() -> {
            var response = given().get("/journal/" + processInstanceId);
            return response.statusCode() == 200 && Boolean.TRUE.equals(response.jsonPath().get("completed"));
        });
        return given().get("/journal/" + processInstanceId).then().statusCode(200).extract().jsonPath();
    }

    /** Mencari user task milik process instance tertentu lewat {@code GET /usertasks/instance}. */
    static Map<String, Object> awaitUserTask(String processInstanceId) {
        return await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(100))
                .until(() -> findUserTask(processInstanceId), Objects::nonNull);
    }

    private static Map<String, Object> findUserTask(String processInstanceId) {
        List<Map<String, Object>> tasks = given()
                .queryParam("user", OFFICER)
                .queryParam("group", OFFICER_GROUP)
                .when().get("/usertasks/instance")
                .then().statusCode(200)
                .extract().jsonPath().getList("$");
        return tasks.stream()
                .filter(t -> {
                    Object metadata = t.get("metadata");
                    return metadata instanceof Map<?, ?> m && processInstanceId.equals(m.get("ProcessInstanceId"));
                })
                .findFirst()
                .orElse(null);
    }

    /** Menjalankan satu transisi lifecycle user task (claim, complete, release, skip...). */
    static JsonPath transition(String taskId, String transitionId, Map<String, Object> data) {
        return given().contentType(ContentType.JSON)
                .queryParam("user", OFFICER)
                .queryParam("group", OFFICER_GROUP)
                .body(Map.of("transitionId", transitionId, "data", data))
                .when().post("/usertasks/instance/" + taskId + "/transition")
                .then().statusCode(200)
                .extract().jsonPath();
    }
}
