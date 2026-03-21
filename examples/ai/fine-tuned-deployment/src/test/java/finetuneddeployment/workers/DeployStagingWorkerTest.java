package finetuneddeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeployStagingWorkerTest {

    private final DeployStagingWorker worker = new DeployStagingWorker();

    @Test
    void taskDefName() {
        assertEquals("ftd_deploy_staging", worker.getTaskDefName());
    }

    @Test
    void deploysToStagingEndpoint() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "support-bot-v3",
                "checksum", "sha256:a1b2c3d4e5f6")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://staging.models.internal/support-bot-v3", result.getOutputData().get("endpoint"));
        assertEquals("running", result.getOutputData().get("status"));
        assertEquals("A100", result.getOutputData().get("gpuType"));
        assertEquals(2, result.getOutputData().get("replicas"));
    }

    @Test
    void endpointIncludesModelId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "my-model",
                "checksum", "sha256:abc")));
        TaskResult result = worker.execute(task);

        assertEquals("https://staging.models.internal/my-model", result.getOutputData().get("endpoint"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
