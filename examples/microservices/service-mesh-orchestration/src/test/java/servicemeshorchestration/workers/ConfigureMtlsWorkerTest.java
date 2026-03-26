package servicemeshorchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigureMtlsWorkerTest {

    private final ConfigureMtlsWorker worker = new ConfigureMtlsWorker();

    @Test
    void taskDefName() {
        assertEquals("mesh_configure_mtls", worker.getTaskDefName());
    }

    @Test
    void enablesMtls() {
        Task task = taskWith(Map.of("serviceName", "payment-service", "sidecarId", "envoy-123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("enabled"));
    }

    @Test
    void returnsCertExpiry() {
        Task task = taskWith(Map.of("serviceName", "svc", "sidecarId", "envoy-1"));
        TaskResult result = worker.execute(task);
        assertEquals("2027-01-01", result.getOutputData().get("certExpiry"));
    }

    @Test
    void handlesNullServiceName() {
        Map<String, Object> input = new HashMap<>();
        input.put("serviceName", null);
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
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("serviceName", "svc"));
        TaskResult result = worker.execute(task);
        assertTrue(result.getOutputData().containsKey("enabled"));
        assertTrue(result.getOutputData().containsKey("certExpiry"));
    }

    @Test
    void isDeterministic() {
        Task t1 = taskWith(Map.of("serviceName", "a"));
        Task t2 = taskWith(Map.of("serviceName", "b"));
        assertEquals(worker.execute(t1).getOutputData().get("enabled"),
                     worker.execute(t2).getOutputData().get("enabled"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
