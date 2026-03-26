package patchmanagement.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeployPatchTest {

    private final DeployPatch worker = new DeployPatch();

    @Test
    void taskDefName() {
        assertEquals("pm_deploy_patch", worker.getTaskDefName());
    }

    @Test
    void deploysAfterStagingPassed() {
        Task task = taskWith(Map.of("stagingPassed", true));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deployed"));
        assertEquals(24, result.getOutputData().get("hostsPatched"));
        assertEquals("rolling", result.getOutputData().get("deploymentStrategy"));
    }

    @Test
    void deploysEvenIfStagingFalse() {
        Task task = taskWith(Map.of("stagingPassed", false));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("deployed"));
    }

    @Test
    void rollingDeploymentStrategy() {
        Task task = taskWith(Map.of("stagingPassed", true));
        TaskResult result = worker.execute(task);

        assertEquals("rolling", result.getOutputData().get("deploymentStrategy"));
    }

    @Test
    void handlesNullData() {
        Map<String, Object> input = new HashMap<>();
        input.put("deploy_patchData", null);
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void processedIsTrue() {
        Task task = taskWith(Map.of("stagingPassed", true));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsAllFields() {
        Task task = taskWith(Map.of("stagingPassed", true));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("deployed"));
        assertNotNull(result.getOutputData().get("hostsPatched"));
        assertNotNull(result.getOutputData().get("deploymentStrategy"));
        assertNotNull(result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> dataMap) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("deploy_patchData", dataMap);
        task.setInputData(input);
        return task;
    }
}
