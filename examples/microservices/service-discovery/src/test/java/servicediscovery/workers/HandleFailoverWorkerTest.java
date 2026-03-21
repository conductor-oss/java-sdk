package servicediscovery.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HandleFailoverWorkerTest {

    private final HandleFailoverWorker worker = new HandleFailoverWorker();

    @Test
    void taskDefName() {
        assertEquals("sd_handle_failover", worker.getTaskDefName());
    }

    @Test
    void successfulCallNoFailover() {
        Task task = taskWith(Map.of("response", Map.of("data", Map.of("orderId", "ORD-555")),
                "instance", Map.of("id", "inst-02"), "allInstances", java.util.List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("failoverTriggered"));
    }

    @Test
    void returnsDataOnSuccess() {
        Task task = taskWith(Map.of("response", Map.of("data", Map.of("orderId", "ORD-555", "status", "processed")),
                "instance", Map.of("id", "inst-02"), "allInstances", java.util.List.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> finalResult = (Map<String, Object>) result.getOutputData().get("finalResult");
        assertEquals("ORD-555", finalResult.get("orderId"));
    }

    @Test
    void failedCallTriggersFailover() {
        Task task = taskWith(Map.of("response", Map.of("error", "timeout"),
                "instance", Map.of("id", "inst-01"), "allInstances", java.util.List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("failoverTriggered"));
    }

    @Test
    void handlesNullResponse() {
        Map<String, Object> input = new HashMap<>();
        input.put("response", null);
        input.put("instance", Map.of("id", "inst-01"));
        input.put("allInstances", java.util.List.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("failoverTriggered"));
    }

    @Test
    void handlesMissingResponse() {
        Task task = taskWith(Map.of("instance", Map.of("id", "inst-01")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("failoverTriggered"));
    }

    @Test
    void emptyResponseDataTriggersFailover() {
        Map<String, Object> response = new HashMap<>();
        response.put("data", null);
        Task task = taskWith(Map.of("response", response, "instance", Map.of("id", "inst-01")));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("failoverTriggered"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of("response", Map.of("data", Map.of("orderId", "ORD-555")),
                "instance", Map.of("id", "inst-02")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("finalResult"));
        assertTrue(result.getOutputData().containsKey("failoverTriggered"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
