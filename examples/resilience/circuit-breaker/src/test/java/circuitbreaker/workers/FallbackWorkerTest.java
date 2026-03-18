package circuitbreaker.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FallbackWorkerTest {

    @Test
    void taskDefName() {
        FallbackWorker worker = new FallbackWorker();
        assertEquals("cb_fallback", worker.getTaskDefName());
    }

    @Test
    void returnsFallbackData() {
        FallbackWorker worker = new FallbackWorker();
        Task task = taskWith(Map.of("serviceName", "payment-api"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Fallback data for payment-api", result.getOutputData().get("result"));
        assertEquals("cache", result.getOutputData().get("source"));
        assertEquals("payment-api", result.getOutputData().get("serviceName"));
    }

    @Test
    void usesDefaultServiceNameWhenNotProvided() {
        FallbackWorker worker = new FallbackWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Fallback data for default-service", result.getOutputData().get("result"));
        assertEquals("cache", result.getOutputData().get("source"));
        assertEquals("default-service", result.getOutputData().get("serviceName"));
    }

    @Test
    void outputContainsRequiredFields() {
        FallbackWorker worker = new FallbackWorker();
        Task task = taskWith(Map.of("serviceName", "inventory-api"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("source"));
        assertTrue(result.getOutputData().containsKey("serviceName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
