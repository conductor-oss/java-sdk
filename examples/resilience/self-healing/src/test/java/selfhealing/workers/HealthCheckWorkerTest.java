package selfhealing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthCheckWorkerTest {

    @Test
    void taskDefName() {
        HealthCheckWorker worker = new HealthCheckWorker();
        assertEquals("sh_health_check", worker.getTaskDefName());
    }

    @Test
    void healthyServiceReturnsTrue() {
        HealthCheckWorker worker = new HealthCheckWorker();
        Task task = taskWith(Map.of("service", "my-api"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("healthy"));
        assertEquals("my-api", result.getOutputData().get("service"));
        assertFalse(result.getOutputData().containsKey("symptoms"));
    }

    @Test
    void brokenServiceReturnsFalseWithSymptoms() {
        HealthCheckWorker worker = new HealthCheckWorker();
        Task task = taskWith(Map.of("service", "broken-service"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("false", result.getOutputData().get("healthy"));
        assertEquals("broken-service", result.getOutputData().get("service"));
        assertEquals("connection_timeouts", result.getOutputData().get("symptoms"));
    }

    @Test
    void healthyOutputIsStringNotBoolean() {
        HealthCheckWorker worker = new HealthCheckWorker();

        Task healthyTask = taskWith(Map.of("service", "good-service"));
        TaskResult healthyResult = worker.execute(healthyTask);
        assertInstanceOf(String.class, healthyResult.getOutputData().get("healthy"));

        Task brokenTask = taskWith(Map.of("service", "broken-service"));
        TaskResult brokenResult = worker.execute(brokenTask);
        assertInstanceOf(String.class, brokenResult.getOutputData().get("healthy"));
    }

    @Test
    void nullServiceTreatedAsHealthy() {
        HealthCheckWorker worker = new HealthCheckWorker();
        Task task = taskWith(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("healthy"));
    }

    @Test
    void emptyServiceTreatedAsHealthy() {
        HealthCheckWorker worker = new HealthCheckWorker();
        Task task = taskWith(Map.of("service", ""));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("healthy"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
