package datadedup.workers;

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
        assertEquals("dp_load_records", worker.getTaskDefName());
    }

    @Test
    void loadsRecordsAndReturnsCount() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice"),
                Map.of("id", 2, "name", "Bob"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(records, result.getOutputData().get("records"));
        assertEquals(2, result.getOutputData().get("count"));
    }

    @Test
    void handlesEmptyRecordsList() {
        Task task = taskWith(Map.of("records", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(List.of(), result.getOutputData().get("records"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullRecords() {
        Map<String, Object> input = new HashMap<>();
        input.put("records", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(List.of(), result.getOutputData().get("records"));
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void singleRecord() {
        List<Map<String, Object>> records = List.of(Map.of("id", 1, "name", "Solo"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("count"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputRecords =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals(1, outputRecords.size());
    }

    @Test
    void preservesRecordFields() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com"));
        Task task = taskWith(Map.of("records", records));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputRecords =
                (List<Map<String, Object>>) result.getOutputData().get("records");
        assertEquals("alice@example.com", outputRecords.get(0).get("email"));
    }

    @Test
    void multipleRecordsCount() {
        List<Map<String, Object>> records = List.of(
                Map.of("id", 1), Map.of("id", 2), Map.of("id", 3),
                Map.of("id", 4), Map.of("id", 5));
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
