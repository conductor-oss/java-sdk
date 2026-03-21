package gitopsworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DetectDriftWorkerTest {

    private final DetectDriftWorker worker = new DetectDriftWorker();

    @Test
    void taskDefName() {
        assertEquals("go_detect_drift", worker.getTaskDefName());
    }

    @Test
    void detectsDriftWithValidInputs() {
        Task task = taskWith(Map.of("repository", "infra-manifests", "targetCluster", "prod-east"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("DETECT_DRIFT-1361", result.getOutputData().get("detect_driftId"));
        assertEquals(true, result.getOutputData().get("success"));
        assertEquals("infra-manifests", result.getOutputData().get("repository"));
        assertEquals("prod-east", result.getOutputData().get("targetCluster"));
        assertEquals(3, result.getOutputData().get("driftedResources"));
    }

    @Test
    void handlesNullRepository() {
        Map<String, Object> input = new HashMap<>();
        input.put("repository", null);
        input.put("targetCluster", "prod-west");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-repo", result.getOutputData().get("repository"));
    }

    @Test
    void handlesNullTargetCluster() {
        Map<String, Object> input = new HashMap<>();
        input.put("repository", "my-repo");
        input.put("targetCluster", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-cluster", result.getOutputData().get("targetCluster"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown-repo", result.getOutputData().get("repository"));
        assertEquals("unknown-cluster", result.getOutputData().get("targetCluster"));
    }

    @Test
    void outputContainsDriftedResources() {
        Task task = taskWith(Map.of("repository", "repo", "targetCluster", "cluster"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("driftedResources"));
    }

    @Test
    void outputContainsDetectDriftId() {
        Task task = taskWith(Map.of("repository", "repo", "targetCluster", "cluster"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("detect_driftId"));
    }

    @Test
    void alwaysReturnsSuccess() {
        Task task = taskWith(Map.of("repository", "any", "targetCluster", "any"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
