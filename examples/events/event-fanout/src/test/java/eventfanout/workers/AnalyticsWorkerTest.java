package eventfanout.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsWorkerTest {

    private final AnalyticsWorker worker = new AnalyticsWorker();

    @Test
    void taskDefName() {
        assertEquals("fo_analytics", worker.getTaskDefName());
    }

    @Test
    void tracksAnalyticsForEvent() {
        Task task = taskWith(Map.of(
                "eventId", "evt-001",
                "payload", Map.of("orderId", "ORD-100")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("tracked", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("metricsUpdated"));
    }

    @Test
    void outputResultIsTracked() {
        Task task = taskWith(Map.of(
                "eventId", "evt-002",
                "payload", Map.of("amount", 500)));
        TaskResult result = worker.execute(task);

        assertEquals("tracked", result.getOutputData().get("result"));
    }

    @Test
    void outputMetricsUpdatedIsTrue() {
        Task task = taskWith(Map.of(
                "eventId", "evt-003",
                "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("metricsUpdated"));
    }

    @Test
    void handlesNullEventId() {
        Map<String, Object> input = new HashMap<>();
        input.put("eventId", null);
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("tracked", result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingEventId() {
        Task task = taskWith(Map.of("payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("tracked", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("metricsUpdated"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("tracked", result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("metricsUpdated"));
    }

    @Test
    void handlesLargePayload() {
        Map<String, Object> payload = Map.of(
                "field1", "value1",
                "field2", "value2",
                "field3", 12345);
        Task task = taskWith(Map.of(
                "eventId", "evt-big",
                "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("tracked", result.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
