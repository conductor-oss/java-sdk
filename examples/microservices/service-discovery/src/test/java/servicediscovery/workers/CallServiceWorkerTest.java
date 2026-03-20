package servicediscovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CallServiceWorkerTest {

    private final CallServiceWorker worker = new CallServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_call_service", worker.getTaskDefName());
    }

    @Test
    void callsServiceSuccessfully() {
        Task task = taskWith(Map.of("instance", Map.of("id", "inst-02", "host", "10.0.1.11", "port", 8080), "request", Map.of("action", "getOrder")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("success"));
        assertEquals(23, result.getOutputData().get("latency"));
    }

    @Test
    void returnsOrderInResponse() {
        Task task = taskWith(Map.of("instance", Map.of("id", "inst-02", "host", "10.0.1.11", "port", 8080)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) result.getOutputData().get("response");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        assertEquals("ORD-555", data.get("orderId"));
        assertEquals("processed", data.get("status"));
    }

    @Test
    void returnsSuccessFlag() {
        Task task = taskWith(Map.of("instance", Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080)));
        TaskResult result = worker.execute(task);

        assertTrue((Boolean) result.getOutputData().get("success"));
    }

    @Test
    void returnsLatency() {
        Task task = taskWith(Map.of("instance", Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080)));
        TaskResult result = worker.execute(task);

        assertEquals(23, result.getOutputData().get("latency"));
    }

    @Test
    void handlesNullInstance() {
        Map<String, Object> input = new HashMap<>();
        input.put("instance", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInstance() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("instance", Map.of("id", "inst-01", "host", "10.0.1.10", "port", 8080)));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("latency"));
        assertTrue(result.getOutputData().containsKey("success"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
