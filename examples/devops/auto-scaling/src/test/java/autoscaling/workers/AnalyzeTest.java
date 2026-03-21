package autoscaling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeTest {

    private final Analyze worker = new Analyze();

    @Test
    void taskDefName() {
        assertEquals("as_analyze", worker.getTaskDefName());
    }

    @Test
    void apiServerReturnsHighLoad() {
        Task task = taskWith("api-server", "cpu");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(85, result.getOutputData().get("currentLoad"));
        assertEquals("api-server", result.getOutputData().get("service"));
    }

    @Test
    void webFrontendReturnsModerateLoad() {
        Task task = taskWith("web-frontend", "cpu");
        TaskResult result = worker.execute(task);

        assertEquals(45, result.getOutputData().get("currentLoad"));
    }

    @Test
    void batchWorkerReturnsVeryHighLoad() {
        Task task = taskWith("batch-worker", "memory");
        TaskResult result = worker.execute(task);

        assertEquals(92, result.getOutputData().get("currentLoad"));
        assertEquals("memory", result.getOutputData().get("metric"));
    }

    @Test
    void cacheServiceReturnsLowLoad() {
        Task task = taskWith("cache-service", "cpu");
        TaskResult result = worker.execute(task);

        assertEquals(30, result.getOutputData().get("currentLoad"));
    }

    @Test
    void unknownServiceReturnsDefaultLoad() {
        Task task = taskWith("unknown-svc", "cpu");
        TaskResult result = worker.execute(task);

        assertEquals(60, result.getOutputData().get("currentLoad"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith("api-server", "cpu");
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("service"));
        assertNotNull(result.getOutputData().get("metric"));
        assertNotNull(result.getOutputData().get("currentLoad"));
        assertNotNull(result.getOutputData().get("avgLoad15m"));
        assertNotNull(result.getOutputData().get("peakLoad1h"));
        assertNotNull(result.getOutputData().get("currentInstances"));
    }

    @Test
    void avgLoadIsLessThanCurrentLoad() {
        Task task = taskWith("api-server", "cpu");
        TaskResult result = worker.execute(task);

        int current = (int) result.getOutputData().get("currentLoad");
        int avg = (int) result.getOutputData().get("avgLoad15m");
        assertTrue(avg < current);
    }

    @Test
    void peakLoadIsGreaterThanCurrentLoad() {
        Task task = taskWith("api-server", "cpu");
        TaskResult result = worker.execute(task);

        int current = (int) result.getOutputData().get("currentLoad");
        int peak = (int) result.getOutputData().get("peakLoad1h");
        assertTrue(peak > current);
    }

    @Test
    void nullServiceDefaultsGracefully() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-service", result.getOutputData().get("service"));
        assertEquals(60, result.getOutputData().get("currentLoad"));
    }

    private Task taskWith(String service, String metric) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("service", service);
        input.put("metric", metric);
        task.setInputData(input);
        return task;
    }
}
