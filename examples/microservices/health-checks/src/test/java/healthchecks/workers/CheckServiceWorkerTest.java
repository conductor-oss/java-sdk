package healthchecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckServiceWorkerTest {

    private final CheckServiceWorker worker = new CheckServiceWorker();

    @Test
    void taskDefName() {
        assertEquals("hc_check_service", worker.getTaskDefName());
    }

    @Test
    void checksApiGateway() {
        Task task = taskWith(Map.of("serviceName", "api-gateway", "endpoint", "http://api-gateway:8080/health"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result.getOutputData().get("health");
        assertNotNull(health);
        assertEquals("api-gateway", health.get("service"));
        assertEquals("healthy", health.get("status"));
        assertEquals(12, health.get("latencyMs"));
    }

    @Test
    void checksDatabase() {
        Task task = taskWith(Map.of("serviceName", "database", "endpoint", "http://database:5432/health"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result.getOutputData().get("health");
        assertEquals("database", health.get("service"));
        assertEquals("healthy", health.get("status"));
    }

    @Test
    void unknownServiceReturnsUnknown() {
        Task task = taskWith(Map.of("serviceName", "unknown-svc", "endpoint", "http://x:80/health"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result.getOutputData().get("health");
        assertEquals("unknown", health.get("status"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
