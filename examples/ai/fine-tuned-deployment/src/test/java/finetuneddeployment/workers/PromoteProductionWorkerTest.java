package finetuneddeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromoteProductionWorkerTest {

    private final PromoteProductionWorker worker = new PromoteProductionWorker();

    @Test
    void taskDefName() {
        assertEquals("ftd_promote_production", worker.getTaskDefName());
    }

    @Test
    void promotesToProduction() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "support-bot-v3",
                "stagingEndpoint", "https://staging.models.internal/support-bot-v3")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("https://api.models.internal/support-bot-v3", result.getOutputData().get("prodEndpoint"));
        assertEquals("live", result.getOutputData().get("status"));
        assertEquals(100, result.getOutputData().get("trafficPercent"));
    }

    @Test
    void prodEndpointIncludesModelId() {
        Task task = taskWith(new HashMap<>(Map.of(
                "modelId", "my-model",
                "stagingEndpoint", "https://staging.models.internal/my-model")));
        TaskResult result = worker.execute(task);

        assertEquals("https://api.models.internal/my-model", result.getOutputData().get("prodEndpoint"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
