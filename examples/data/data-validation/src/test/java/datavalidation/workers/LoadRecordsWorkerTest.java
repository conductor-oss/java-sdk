package datavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadRecordsWorkerTest {

    private final LoadRecordsWorker worker = new LoadRecordsWorker();

    @Test
    void taskDefName() {
        assertEquals("vd_load_records", worker.getTaskDefName());
    }

    @Test
    void loadsMultipleRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "alice@example.com", "age", 30),
                Map.of("name", "Bob", "email", "bob@example.com", "age", 25));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("count"));
        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void loadsSingleRecord() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "email", "alice@example.com", "age", 30));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("count"));
    }

    @Test
    void handlesEmptyRecordsList() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingRecordsKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesRecordData() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Charlie", "email", "charlie@test.com", "age", 42));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> outputRecords = (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, outputRecords.size());
        assertEquals("Charlie", outputRecords.get(0).get("name"));
        assertEquals("charlie@test.com", outputRecords.get(0).get("email"));
        assertEquals(42, outputRecords.get(0).get("age"));
    }

    @Test
    void countMatchesRecordSize() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "A"), Map.of("name", "B"), Map.of("name", "C"),
                Map.of("name", "D"), Map.of("name", "E"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
