package serviceregistry.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthCheckWorkerTest {

    private final HealthCheckWorker worker = new HealthCheckWorker();

    @Test
    void taskDefName() {
        assertEquals("sr_health_check", worker.getTaskDefName());
    }

    @Test
    void returnsHealthyTrue() {
        Task task = taskWith(Map.of("serviceUrl", "http://order-svc:8080"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("healthy"));
    }

    @Test
    void returnsLatencyMs() {
        Task task = taskWith(Map.of("serviceUrl", "http://order-svc:8080"));
        TaskResult result = worker.execute(task);

        assertEquals(12, result.getOutputData().get("latencyMs"));
    }

    @Test
    void handlesNullServiceUrl() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceUrl", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("healthy"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("serviceUrl", "http://svc:3000"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("healthy"));
        assertTrue(result.getOutputData().containsKey("latencyMs"));
    }

    @Test
    void differentUrlsSameResult() {
        Task task1 = taskWith(Map.of("serviceUrl", "http://svc-a:8080"));
        Task task2 = taskWith(Map.of("serviceUrl", "http://svc-b:9090"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("healthy"), r2.getOutputData().get("healthy"));
        assertEquals(r1.getOutputData().get("latencyMs"), r2.getOutputData().get("latencyMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
