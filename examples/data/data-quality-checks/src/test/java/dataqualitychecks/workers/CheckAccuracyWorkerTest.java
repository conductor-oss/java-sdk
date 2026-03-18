package dataqualitychecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckAccuracyWorkerTest {

    private final CheckAccuracyWorker worker = new CheckAccuracyWorker();

    @Test
    void taskDefName() {
        assertEquals("qc_check_accuracy", worker.getTaskDefName());
    }

    @Test
    void allRecordsAccurate() {
        List<Map<String, Object>> records = List.of(
                Map.of("email", "alice@test.com", "status", "active"),
                Map.of("email", "bob@test.com", "status", "inactive"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1.0, result.getOutputData().get("score"));
        assertEquals(2, result.getOutputData().get("accurateCount"));
    }

    @Test
    void invalidEmailReducesScore() {
        List<Map<String, Object>> records = List.of(
                Map.of("email", "invalid-email", "status", "active"),
                Map.of("email", "valid@test.com", "status", "active"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("score"));
        assertEquals(1, result.getOutputData().get("accurateCount"));
    }

    @Test
    void invalidStatusReducesScore() {
        List<Map<String, Object>> records = List.of(
                Map.of("email", "a@b.com", "status", "unknown"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("accurateCount"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, result.getOutputData().get("score"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, result.getOutputData().get("score"));
    }

    @Test
    void validatesEmailWithRegex() {
        List<Map<String, Object>> records = List.of(
                Map.of("email", "user@domain.com", "status", "active"),   // valid
                Map.of("email", "user@domain", "status", "active"),       // invalid: no TLD
                Map.of("email", "@domain.com", "status", "active"),       // invalid: no local part
                Map.of("email", "user.name+tag@domain.co.uk", "status", "active")); // valid
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("accurateCount"));
    }

    @Test
    void validatesPhoneFormat() {
        List<Map<String, Object>> records = List.of(
                Map.of("email", "a@b.com", "status", "active", "phone", "555-123-4567"),  // valid
                Map.of("email", "b@b.com", "status", "active", "phone", "abc-not-phone")); // invalid
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("accurateCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void reportsIssueDetails() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "not-valid", "status", "bogus"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> issues =
                (List<Map<String, Object>>) result.getOutputData().get("issues");
        assertNotNull(issues);
        assertEquals(1, issues.size());
        List<String> recordIssues = (List<String>) issues.get(0).get("issues");
        assertTrue(recordIssues.stream().anyMatch(s -> s.contains("email")));
        assertTrue(recordIssues.stream().anyMatch(s -> s.contains("status")));
    }

    @Test
    void pendingIsValidStatus() {
        List<Map<String, Object>> records = List.of(
                Map.of("email", "user@test.com", "status", "pending"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("accurateCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
