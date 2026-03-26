package webhooktrigger.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StoreResultWorkerTest {

    private final StoreResultWorker worker = new StoreResultWorker();

    @Test
    void taskDefName() {
        assertEquals("wt_store_result", worker.getTaskDefName());
    }

    @Test
    void storesResultSuccessfully() {
        Task task = taskWith(new HashMap<>(Map.of(
                "transformedData", Map.of("id", "ORD-2026-4821", "account", "acme-corp"),
                "destination", "orders_db")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsFixedRecordId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "transformedData", Map.of("id", "ORD-2026-4821"),
                "destination", "orders_db")));
        TaskResult result = worker.execute(task);

        assertEquals("rec-fixed-001", result.getOutputData().get("recordId"));
    }

    @Test
    void returnsFixedStoredAt() {
        Task task = taskWith(new HashMap<>(Map.of(
                "transformedData", Map.of("id", "ORD-2026-4821"),
                "destination", "orders_db")));
        TaskResult result = worker.execute(task);

        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("storedAt"));
    }

    @Test
    void returnsDestination() {
        Task task = taskWith(new HashMap<>(Map.of(
                "transformedData", Map.of("id", "ORD-2026-4821"),
                "destination", "orders_db")));
        TaskResult result = worker.execute(task);

        assertEquals("orders_db", result.getOutputData().get("destination"));
    }

    @Test
    void handlesNullTransformedData() {
        Map<String, Object> input = new HashMap<>();
        input.put("transformedData", null);
        input.put("destination", "orders_db");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rec-fixed-001", result.getOutputData().get("recordId"));
    }

    @Test
    void handlesNullDestination() {
        Map<String, Object> input = new HashMap<>();
        input.put("transformedData", Map.of("id", "ORD-2026-4821"));
        input.put("destination", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default_db", result.getOutputData().get("destination"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rec-fixed-001", result.getOutputData().get("recordId"));
        assertEquals("2026-01-15T10:00:00Z", result.getOutputData().get("storedAt"));
        assertEquals("default_db", result.getOutputData().get("destination"));
    }

    @Test
    void handlesEmptyDestination() {
        Task task = taskWith(new HashMap<>(Map.of(
                "transformedData", Map.of("id", "ORD-2026-4821"),
                "destination", "")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("default_db", result.getOutputData().get("destination"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
