package canaryrelease.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DeployCanaryWorkerTest {
    private final DeployCanaryWorker worker = new DeployCanaryWorker();

    @Test void taskDefName() { assertEquals("cy_deploy_canary", worker.getTaskDefName()); }

    @Test void deploysCanary() {
        Task task = taskWith(Map.of("appName", "user-service", "version", "3.1.0", "trafficPercent", 5));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1, r.getOutputData().get("canaryInstances"));
    }

    @Test void handlesMissingInputs() {
        assertEquals(TaskResult.Status.COMPLETED, worker.execute(taskWith(Map.of())).getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
