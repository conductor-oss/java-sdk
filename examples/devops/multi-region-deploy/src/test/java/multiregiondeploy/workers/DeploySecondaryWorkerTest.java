package multiregiondeploy.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeploySecondaryWorkerTest {

    private final DeploySecondaryWorker worker = new DeploySecondaryWorker();

    @Test
    void taskDefName() {
        assertEquals("mrd_deploy_secondary", worker.getTaskDefName());
    }

    @Test
    void deploysSecondarySuccessfully() {
        Task task = taskWith(Map.of("deploy_secondaryData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deploy_secondary"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsRegions() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals("eu-west-1, ap-southeast-1", result.getOutputData().get("regions"));
    }

    @Test
    void alwaysReturnsDeploySecondary() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("deploy_secondary"));
    }

    @Test
    void alwaysReturnsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void statusIsCompleted() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("deploy_secondary"));
        assertTrue(result.getOutputData().containsKey("processed"));
        assertTrue(result.getOutputData().containsKey("regions"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
