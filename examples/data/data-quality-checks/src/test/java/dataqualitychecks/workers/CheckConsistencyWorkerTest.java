package dataqualitychecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckConsistencyWorkerTest {

    private final CheckConsistencyWorker worker = new CheckConsistencyWorker();

    @Test
    void taskDefName() {
        assertEquals("qc_check_consistency", worker.getTaskDefName());
    }

    @Test
    void noDuplicates() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1), Map.of("id", 2), Map.of("id", 3));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1.0, result.getOutputData().get("score"));
        assertEquals(0, result.getOutputData().get("duplicates"));
    }

    @Test
    void withDuplicateIds() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1), Map.of("id", 1), Map.of("id", 2));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        int duplicates = ((Number) result.getOutputData().get("duplicates")).intValue();
        assertTrue(duplicates >= 1);
        double score = ((Number) result.getOutputData().get("score")).doubleValue();
        assertTrue(score < 1.0);
    }

    @Test
    void allDuplicateIds() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1), Map.of("id", 1), Map.of("id", 1));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        int duplicates = ((Number) result.getOutputData().get("duplicates")).intValue();
        assertEquals(2, duplicates);
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
        assertEquals(0, result.getOutputData().get("duplicates"));
    }

    @Test
    void detectsDuplicateEmails() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "alice@test.com"),
                Map.of("id", 2, "email", "alice@test.com"),
                Map.of("id", 3, "email", "bob@test.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        int duplicates = ((Number) result.getOutputData().get("duplicates")).intValue();
        assertTrue(duplicates >= 1);
    }

    @Test
    void emailDuplicationIsCaseInsensitive() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "Alice@Test.com"),
                Map.of("id", 2, "email", "alice@test.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        int duplicates = ((Number) result.getOutputData().get("duplicates")).intValue();
        assertEquals(1, duplicates); // should detect case-insensitive dupe
    }

    @SuppressWarnings("unchecked")
    @Test
    void reportsDuplicateDetails() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "alice@test.com"),
                Map.of("id", 1, "email", "bob@test.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> details =
                (List<Map<String, Object>>) result.getOutputData().get("duplicateDetails");
        assertNotNull(details);
        assertFalse(details.isEmpty());
        assertEquals("duplicate_id", details.get(0).get("type"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
