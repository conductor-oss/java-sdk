package servicemeshorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeploySidecarWorkerTest {

    private final DeploySidecarWorker worker = new DeploySidecarWorker();

    @Test
    void taskDefName() {
        assertEquals("mesh_deploy_sidecar", worker.getTaskDefName());
    }

    @Test
    void deploysSidecarSuccessfully() {
        Task task = taskWith(Map.of("serviceName", "payment-service", "namespace", "production"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("sidecarId"));
        assertEquals("1.28", result.getOutputData().get("version"));
    }

    @Test
    void sidecarIdIsDeterministic() {
        Task t1 = taskWith(Map.of("serviceName", "svc-a", "namespace", "prod"));
        Task t2 = taskWith(Map.of("serviceName", "svc-a", "namespace", "prod"));
        assertEquals(worker.execute(t1).getOutputData().get("sidecarId"),
                     worker.execute(t2).getOutputData().get("sidecarId"));
    }

    @Test
    void differentServicesGetDifferentIds() {
        Task t1 = taskWith(Map.of("serviceName", "svc-a", "namespace", "prod"));
        Task t2 = taskWith(Map.of("serviceName", "svc-b", "namespace", "prod"));
        assertNotEquals(worker.execute(t1).getOutputData().get("sidecarId"),
                        worker.execute(t2).getOutputData().get("sidecarId"));
    }

    @Test
    void handlesNullServiceName() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", null);
        input.put("namespace", "prod");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullNamespace() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", "svc");
        input.put("namespace", null);
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
    void sidecarIdStartsWithEnvoy() {
        Task task = taskWith(Map.of("serviceName", "test-svc", "namespace", "ns"));
        TaskResult result = worker.execute(task);
        String id = (String) result.getOutputData().get("sidecarId");
        assertTrue(id.startsWith("envoy-"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
