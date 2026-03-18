package dataenrichment.workers;

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
        assertEquals("dr_load_records", worker.getTaskDefName());
    }

    @Test
    void loadsMultipleRecords() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob"),
                Map.of("id", 3, "name", "Charlie"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("count"));
        assertNotNull(result.getOutputData().get("records"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void preservesRecordData() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@acme.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> outputRecords =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, outputRecords.size());
        assertEquals("Alice", outputRecords.get(0).get("name"));
        assertEquals("alice@acme.com", outputRecords.get(0).get("email"));
    }

    @Test
    void countMatchesRecordSize() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "A"),
                Map.of("id", 2, "name", "B"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    void handlesSingleRecord() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Solo"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
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
        assertNotNull(result.getOutputData().get("records"));
    }

    @Test
    void handlesMissingRecordsKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
        assertNotNull(result.getOutputData().get("records"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
