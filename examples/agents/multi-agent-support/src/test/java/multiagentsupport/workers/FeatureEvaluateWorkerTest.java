package multiagentsupport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeatureEvaluateWorkerTest {

    private final FeatureEvaluateWorker worker = new FeatureEvaluateWorker();

    @Test
    void taskDefName() {
        assertEquals("cs_feature_evaluate", worker.getTaskDefName());
    }

    @Test
    void returnsFeatureEvaluation() {
        Task task = taskWith(Map.of(
                "subject", "Add dark mode",
                "description", "Please add dark mode support",
                "customerTier", "premium"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("feature request"));
    }

    @Test
    void returnsPriorityHigh() {
        Task task = taskWith(Map.of(
                "subject", "New feature",
                "description", "Add export to CSV",
                "customerTier", "standard"));
        TaskResult result = worker.execute(task);

        assertEquals("high", result.getOutputData().get("priority"));
    }

    @Test
    void returnsEtaAndRoadmapId() {
        Task task = taskWith(Map.of(
                "subject", "Feature",
                "description", "Enhancement request",
                "customerTier", "enterprise"));
        TaskResult result = worker.execute(task);

        assertEquals("Q2 2026", result.getOutputData().get("eta"));
        assertEquals("FR-4521", result.getOutputData().get("roadmapId"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("subject", null);
        input.put("description", null);
        input.put("customerTier", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
        assertNotNull(result.getOutputData().get("priority"));
        assertNotNull(result.getOutputData().get("eta"));
        assertNotNull(result.getOutputData().get("roadmapId"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("subject", "Test", "description", "Test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("priority"));
        assertTrue(result.getOutputData().containsKey("eta"));
        assertTrue(result.getOutputData().containsKey("roadmapId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
