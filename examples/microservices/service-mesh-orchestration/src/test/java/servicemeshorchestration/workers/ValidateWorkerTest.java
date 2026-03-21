package servicemeshorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateWorkerTest {

    private final ValidateWorker worker = new ValidateWorker();

    @Test
    void taskDefName() {
        assertEquals("mesh_validate", worker.getTaskDefName());
    }

    @Test
    void validatesSuccessfully() {
        Task task = taskWith(Map.of("serviceName", "svc", "sidecarId", "envoy-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("valid"));
    }

    @Test
    void returnsLatency() {
        Task task = taskWith(Map.of("serviceName", "svc"));
        TaskResult result = worker.execute(task);
        assertEquals(5, result.getOutputData().get("latencyMs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("valid"));
        assertTrue(result.getOutputData().containsKey("latencyMs"));
    }

    @Test
    void isDeterministic() {
        Task t1 = taskWith(Map.of("serviceName", "a"));
        Task t2 = taskWith(Map.of("serviceName", "b"));
        assertEquals(worker.execute(t1).getOutputData().get("valid"),
                     worker.execute(t2).getOutputData().get("valid"));
    }

    @Test
    void latencyIsLow() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        int latency = (int) result.getOutputData().get("latencyMs");
        assertTrue(latency < 100);
    }

    @Test
    void validIsTrue() {
        Task task = taskWith(Map.of("serviceName", "test", "sidecarId", "envoy-test"));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("valid"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
