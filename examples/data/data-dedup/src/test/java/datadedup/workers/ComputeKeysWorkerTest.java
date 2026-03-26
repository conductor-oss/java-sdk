package datadedup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ComputeKeysWorkerTest {

    private final ComputeKeysWorker worker = new ComputeKeysWorker();

    @Test
    void taskDefName() {
        assertEquals("dp_compute_keys", worker.getTaskDefName());
    }

    @Test
    void computesKeyFromSingleField() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "Alice@Example.COM"));
        Task task = taskWith(Map.of("records", records, "matchFields", List.of("email")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertEquals(1, keyed.size());
        assertEquals("alice@example.com", keyed.get(0).get("dedupKey"));
    }

    @Test
    void computesKeyFromMultipleFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "alice@example.com", "phone", "555-0101"));
        Task task = taskWith(Map.of("records", records,
                "matchFields", List.of("email", "phone")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertEquals("alice@example.com|555-0101", keyed.get(0).get("dedupKey"));
    }

    @Test
    void lowercasesAndTrimsValues() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "  BOB@Example.COM  "));
        Task task = taskWith(Map.of("records", records, "matchFields", List.of("email")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertEquals("bob@example.com", keyed.get(0).get("dedupKey"));
    }

    @Test
    void handlesMissingFieldInRecord() {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("email", null);
        List<Map<String, Object>> records = List.of(record);
        Task task = taskWith(Map.of("records", records, "matchFields", List.of("email")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertEquals("", keyed.get(0).get("dedupKey"));
    }

    @Test
    void defaultsMatchFieldsToEmail() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "email", "test@test.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertEquals("test@test.com", keyed.get(0).get("dedupKey"));
    }

    @Test
    void handlesEmptyRecords() {
        Task task = taskWith(Map.of("records", List.of(), "matchFields", List.of("email")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertTrue(keyed.isEmpty());
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        input.put("matchFields", List.of("email"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertTrue(keyed.isEmpty());
    }

    @Test
    void preservesOriginalFieldsInKeyedRecord() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com"));
        Task task = taskWith(Map.of("records", records, "matchFields", List.of("email")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyed =
                (List<Map<String, Object>>) result.getOutputData().get("keyedRecords");
        assertEquals("Alice", keyed.get(0).get("name"));
        assertEquals(1, keyed.get(0).get("id"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
