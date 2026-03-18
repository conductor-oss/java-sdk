package dataenrichment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupCreditWorkerTest {

    private final LookupCreditWorker worker = new LookupCreditWorker();

    @Test
    void taskDefName() {
        assertEquals("dr_lookup_credit", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void addsCreditDataToRecord() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Alice"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals(1, enriched.size());
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        assertNotNull(credit);
    }

    @SuppressWarnings("unchecked")
    @Test
    void computesBaseScoreForMinimalRecord() {
        // Record with only short name, no email, no company, no geo, no phone
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Al"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        int score = ((Number) credit.get("score")).intValue();
        // Base 650, no bonuses for short name or missing fields
        assertEquals(650, score);
        assertEquals("fair", credit.get("tier"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void boostsScoreForEmail() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Al", "email", "al@test.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        int score = ((Number) credit.get("score")).intValue();
        // Base 650 + 30 for email = 680
        assertEquals(680, score);
    }

    @SuppressWarnings("unchecked")
    @Test
    void boostsScoreForLongName() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice Johnson"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        int score = ((Number) credit.get("score")).intValue();
        // Base 650 + 20 for name > 3 chars = 670
        assertEquals(670, score);
    }

    @SuppressWarnings("unchecked")
    @Test
    void excellentTierForFullyEnrichedRecord() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "Alice Johnson");
        record.put("email", "alice@acme.com");
        record.put("phone", "555-1234");
        record.put("company", Map.of("company", "Acme Corp", "industry", "Tech"));
        record.put("geo", Map.of("city", "San Francisco", "state", "CA"));

        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        int score = ((Number) credit.get("score")).intValue();
        // 650 + 30(email) + 40(company) + 30(geo) + 20(name) + 30(phone) = 800
        assertEquals(800, score);
        assertEquals("excellent", credit.get("tier"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void lastCheckedIsToday() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Alice"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        assertEquals(LocalDate.now().toString(), credit.get("lastChecked"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesOriginalFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@acme.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals("Alice", enriched.get(0).get("name"));
        assertEquals("alice@acme.com", enriched.get(0).get("email"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void enrichesMultipleRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob"),
                Map.of("id", 3, "name", "Charlie"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        assertEquals(3, enriched.size());
        for (Map<String, Object> record : enriched) {
            assertNotNull(record.get("credit"));
        }
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enriched"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void differentRecordsGetDifferentScores() {
        Map<String, Object> minimal = new HashMap<>();
        minimal.put("id", 1);
        minimal.put("name", "Al"); // short name

        Map<String, Object> rich = new HashMap<>();
        rich.put("id", 2);
        rich.put("name", "Alice Johnson");
        rich.put("email", "alice@acme.com");
        rich.put("phone", "555-1234");

        List<Map<String, Object>> records = List.of(minimal, rich);
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit1 = (Map<String, Object>) enriched.get(0).get("credit");
        Map<String, Object> credit2 = (Map<String, Object>) enriched.get(1).get("credit");
        int score1 = ((Number) credit1.get("score")).intValue();
        int score2 = ((Number) credit2.get("score")).intValue();
        assertTrue(score2 > score1, "Rich record should have higher score than minimal");
    }

    @SuppressWarnings("unchecked")
    @Test
    void unknownCompanyDoesNotBoostScore() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("name", "Al");
        record.put("company", Map.of("company", "Unknown", "industry", "Unknown"));

        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> enriched =
                (List<Map<String, Object>>) result.getOutputData().get("enriched");
        Map<String, Object> credit = (Map<String, Object>) enriched.get(0).get("credit");
        int score = ((Number) credit.get("score")).intValue();
        assertEquals(650, score); // no bonus for "Unknown" company
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
