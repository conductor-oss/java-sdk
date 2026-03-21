package eventschemavalidation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessValidWorkerTest {

    private final ProcessValidWorker worker = new ProcessValidWorker();

    @Test
    void taskDefName() {
        assertEquals("sv_process_valid", worker.getTaskDefName());
    }

    @Test
    void processesValidEvent() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "order.created", "source", "shop-api",
                        "data", Map.of("orderId", "ORD-100")),
                "schema", "order_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "user.signup", "source", "auth-svc",
                        "data", Map.of("userId", "U-42")),
                "schema", "user_event_v1"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("processed"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithNullEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", null);
        input.put("schema", "order_event_v1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithNullSchema() {
        Map<String, Object> input = new HashMap<>();
        input.put("event", Map.of("type", "t", "source", "s", "data", "d"));
        input.put("schema", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithMinimalEvent() {
        Task task = taskWith(Map.of(
                "event", Map.of("type", "ping"),
                "schema", "minimal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void completesWithLargeEventData() {
        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            data.put("field_" + i, "value_" + i);
        }
        Task task = taskWith(Map.of(
                "event", Map.of("type", "bulk.update", "source", "batch-svc", "data", data),
                "schema", "bulk_event_v1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
