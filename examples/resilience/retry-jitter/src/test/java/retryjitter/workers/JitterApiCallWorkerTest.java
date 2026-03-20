package retryjitter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JitterApiCallWorkerTest {

    @Test
    void taskDefName() {
        JitterApiCallWorker worker = new JitterApiCallWorker();
        assertEquals("jitter_api_call", worker.getTaskDefName());
    }

    @Test
    void returnsOkResultWithJitter() {
        JitterApiCallWorker worker = new JitterApiCallWorker();
        Task task = taskWith(Map.of("endpoint", "https://api.example.com/data"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ok", result.getOutputData().get("result"));
        assertEquals(1, result.getOutputData().get("attempt"));
        assertNotNull(result.getOutputData().get("jitterMs"));
    }

    @Test
    void jitterIsDeterministicForSameEndpoint() {
        JitterApiCallWorker worker1 = new JitterApiCallWorker();
        JitterApiCallWorker worker2 = new JitterApiCallWorker();

        Task task1 = taskWith(Map.of("endpoint", "https://api.example.com/data"));
        Task task2 = taskWith(Map.of("endpoint", "https://api.example.com/data"));

        TaskResult result1 = worker1.execute(task1);
        TaskResult result2 = worker2.execute(task2);

        assertEquals(result1.getOutputData().get("jitterMs"),
                     result2.getOutputData().get("jitterMs"));
    }

    @Test
    void jitterMatchesHashCodeFormula() {
        JitterApiCallWorker worker = new JitterApiCallWorker();
        String endpoint = "https://api.example.com/data";
        Task task = taskWith(Map.of("endpoint", endpoint));

        TaskResult result = worker.execute(task);

        int expectedJitter = Math.abs(endpoint.hashCode() % 500);
        assertEquals(expectedJitter, result.getOutputData().get("jitterMs"));
    }

    @Test
    void differentEndpointsProduceDifferentJitter() {
        JitterApiCallWorker worker = new JitterApiCallWorker();

        Task task1 = taskWith(Map.of("endpoint", "https://api.example.com/users"));
        Task task2 = taskWith(Map.of("endpoint", "https://api.example.com/orders"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        int jitter1 = (int) result1.getOutputData().get("jitterMs");
        int jitter2 = (int) result2.getOutputData().get("jitterMs");

        // Different endpoints should produce different jitter values (extremely likely)
        // Both should be in range [0, 499]
        assertTrue(jitter1 >= 0 && jitter1 < 500);
        assertTrue(jitter2 >= 0 && jitter2 < 500);
    }

    @Test
    void jitterIsInValidRange() {
        JitterApiCallWorker worker = new JitterApiCallWorker();
        Task task = taskWith(Map.of("endpoint", "any-endpoint"));

        TaskResult result = worker.execute(task);

        int jitterMs = (int) result.getOutputData().get("jitterMs");
        assertTrue(jitterMs >= 0, "Jitter must be non-negative");
        assertTrue(jitterMs < 500, "Jitter must be less than 500");
    }

    @Test
    void callCountIncrements() {
        JitterApiCallWorker worker = new JitterApiCallWorker();

        Task task1 = taskWith(Map.of("endpoint", "ep1"));
        TaskResult result1 = worker.execute(task1);
        assertEquals(1, result1.getOutputData().get("attempt"));

        Task task2 = taskWith(Map.of("endpoint", "ep2"));
        TaskResult result2 = worker.execute(task2);
        assertEquals(2, result2.getOutputData().get("attempt"));

        Task task3 = taskWith(Map.of("endpoint", "ep3"));
        TaskResult result3 = worker.execute(task3);
        assertEquals(3, result3.getOutputData().get("attempt"));

        assertEquals(3, worker.getCallCount());
    }

    @Test
    void usesDefaultEndpointWhenMissing() {
        JitterApiCallWorker worker = new JitterApiCallWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ok", result.getOutputData().get("result"));

        int expectedJitter = Math.abs("default".hashCode() % 500);
        assertEquals(expectedJitter, result.getOutputData().get("jitterMs"));
    }

    @Test
    void handlesNonStringEndpointGracefully() {
        JitterApiCallWorker worker = new JitterApiCallWorker();
        Task task = taskWith(Map.of("endpoint", 12345));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // Non-string endpoint falls back to "default"
        int expectedJitter = Math.abs("default".hashCode() % 500);
        assertEquals(expectedJitter, result.getOutputData().get("jitterMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
