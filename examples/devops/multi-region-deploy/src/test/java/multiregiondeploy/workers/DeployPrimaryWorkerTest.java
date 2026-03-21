package multiregiondeploy.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeployPrimaryWorkerTest {

    private final DeployPrimaryWorker worker = new DeployPrimaryWorker();

    @Test
    void taskDefName() {
        assertEquals("mrd_deploy_primary", worker.getTaskDefName());
    }

    @Test
    void deploysWithValidInputs() {
        Task task = taskWith(Map.of("service", "api-gateway", "version", "3.0.0"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("DEPLOY_PRIMARY-1362", result.getOutputData().get("deploy_primaryId"));
        assertEquals(true, result.getOutputData().get("success"));
        assertEquals("api-gateway", result.getOutputData().get("service"));
        assertEquals("3.0.0", result.getOutputData().get("version"));
        assertEquals("us-east-1", result.getOutputData().get("region"));
    }

    @Test
    void handlesNullService() {
        Map<String, Object> input = new HashMap<>();
        input.put("service", null);
        input.put("version", "1.0.0");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-service", result.getOutputData().get("service"));
    }

    @Test
    void handlesNullVersion() {
        Map<String, Object> input = new HashMap<>();
        input.put("service", "svc");
        input.put("version", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("0.0.0", result.getOutputData().get("version"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-service", result.getOutputData().get("service"));
        assertEquals("0.0.0", result.getOutputData().get("version"));
    }

    @Test
    void outputContainsRegion() {
        Task task = taskWith(Map.of("service", "svc", "version", "1.0"));
        TaskResult result = worker.execute(task);

        assertEquals("us-east-1", result.getOutputData().get("region"));
    }

    @Test
    void alwaysReturnsSuccess() {
        Task task = taskWith(Map.of("service", "svc", "version", "1.0"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void outputContainsDeployPrimaryId() {
        Task task = taskWith(Map.of("service", "svc", "version", "1.0"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("deploy_primaryId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
