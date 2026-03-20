package serviceregistry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DiscoverServiceWorkerTest {

    private final DiscoverServiceWorker worker = new DiscoverServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_discover_service", worker.getTaskDefName());
    }

    @Test
    void discoversServiceEndpoint() {
        Task task = taskWith(Map.of("serviceName", "order-service", "registrationId", "REG-123", "healthy", "true"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("order-service.internal:8080", result.getOutputData().get("endpoint"));
        assertEquals(3, result.getOutputData().get("instances"));
    }

    @Test
    void endpointIncludesServiceName() {
        Task task = taskWith(Map.of("serviceName", "payment-service", "registrationId", "REG-456", "healthy", "true"));
        TaskResult result = worker.execute(task);

        String endpoint = (String) result.getOutputData().get("endpoint");
        assertTrue(endpoint.startsWith("payment-service"));
    }

    @Test
    void handlesUnhealthyService() {
        Task task = taskWith(Map.of("serviceName", "order-service", "registrationId", "REG-123", "healthy", "false"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("endpoint"));
    }

    @Test
    void handlesNullServiceName() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", null);
        input.put("registrationId", "REG-1");
        input.put("healthy", "true");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown.internal:8080", result.getOutputData().get("endpoint"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullHealthy() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", "svc");
        input.put("healthy", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsThreeInstances() {
        Task task = taskWith(Map.of("serviceName", "any-service"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("instances"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
