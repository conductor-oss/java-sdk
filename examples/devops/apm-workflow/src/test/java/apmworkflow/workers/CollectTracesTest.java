package apmworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectTracesTest {

    private final CollectTraces worker = new CollectTraces();

    @Test
    void taskDefName() {
        assertEquals("apm_collect_traces", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("checkout-service", "last-24h");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsTraceCount() {
        Task task = taskWith("checkout-service", "last-24h");
        TaskResult result = worker.execute(task);
        assertEquals(25000, result.getOutputData().get("traceCount"));
    }

    @Test
    void returnsSampledTraces() {
        Task task = taskWith("checkout-service", "last-24h");
        TaskResult result = worker.execute(task);
        assertEquals(2500, result.getOutputData().get("sampledTraces"));
    }

    @Test
    void returnsTimeRange() {
        Task task = taskWith("checkout-service", "last-1h");
        TaskResult result = worker.execute(task);
        assertEquals("last-1h", result.getOutputData().get("timeRange"));
    }

    @Test
    void handlesNullServiceName() {
        Task task = taskWith(null, "last-24h");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith("api-gateway", "last-12h");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("traceCount"));
        assertNotNull(result.getOutputData().get("sampledTraces"));
        assertNotNull(result.getOutputData().get("timeRange"));
    }

    private Task taskWith(String serviceName, String timeRange) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        if (serviceName != null) input.put("serviceName", serviceName);
        if (timeRange != null) input.put("timeRange", timeRange);
        task.setInputData(input);
        return task;
    }
}
