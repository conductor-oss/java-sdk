package servicemeshorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetTrafficPolicyWorkerTest {

    private final SetTrafficPolicyWorker worker = new SetTrafficPolicyWorker();

    @Test
    void taskDefName() {
        assertEquals("mesh_set_traffic_policy", worker.getTaskDefName());
    }

    @Test
    void appliesPolicy() {
        Task task = taskWith(Map.of("serviceName", "svc", "meshType", "istio"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void returnsRetries() {
        Task task = taskWith(Map.of("meshType", "istio"));
        TaskResult result = worker.execute(task);
        assertEquals(3, result.getOutputData().get("retries"));
    }

    @Test
    void returnsTimeout() {
        Task task = taskWith(Map.of("meshType", "linkerd"));
        TaskResult result = worker.execute(task);
        assertEquals("30s", result.getOutputData().get("timeout"));
    }

    @Test
    void handlesNullMeshType() {
        Map<String, Object> input = new HashMap<>();
        input.put("meshType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("meshType", "consul"));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("applied"));
        assertTrue(result.getOutputData().containsKey("retries"));
        assertTrue(result.getOutputData().containsKey("timeout"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
