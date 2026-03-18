package agentcollaboration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StrategistWorkerTest {

    private final StrategistWorker worker = new StrategistWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_strategist", worker.getTaskDefName());
    }

    @Test
    void returnsStrategyWithName() {
        Task task = taskWith(Map.of("businessContext", "Churn problem"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> strategy =
                (Map<String, Object>) result.getOutputData().get("strategy");
        assertNotNull(strategy);
        assertEquals("Stabilize & Retain", strategy.get("name"));
    }

    @Test
    void strategyHasThesisAndPillars() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> strategy =
                (Map<String, Object>) result.getOutputData().get("strategy");
        assertNotNull(strategy.get("thesis"));
        assertTrue(((String) strategy.get("thesis")).length() > 10);

        @SuppressWarnings("unchecked")
        List<String> pillars = (List<String>) strategy.get("pillars");
        assertNotNull(pillars);
        assertEquals(3, pillars.size());
    }

    @Test
    void returnsFourPriorities() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> priorities =
                (List<Map<String, Object>>) result.getOutputData().get("priorities");
        assertNotNull(priorities);
        assertEquals(4, priorities.size());
    }

    @Test
    void prioritiesHaveRequiredFields() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> priorities =
                (List<Map<String, Object>>) result.getOutputData().get("priorities");

        for (Map<String, Object> priority : priorities) {
            assertTrue(priority.containsKey("rank"), "missing rank");
            assertTrue(priority.containsKey("area"), "missing area");
            assertTrue(priority.containsKey("effort"), "missing effort");
            assertTrue(priority.containsKey("impact"), "missing impact");
        }
    }

    @Test
    void prioritiesAreRankedSequentially() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> priorities =
                (List<Map<String, Object>>) result.getOutputData().get("priorities");

        for (int i = 0; i < priorities.size(); i++) {
            assertEquals(i + 1, priorities.get(i).get("rank"));
        }
    }

    @Test
    void returnsStrategyName() {
        Task task = taskWith(Map.of("businessContext", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals("Stabilize & Retain", result.getOutputData().get("strategyName"));
    }

    @Test
    void handlesNullBusinessContext() {
        Map<String, Object> input = new HashMap<>();
        input.put("businessContext", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("strategy"));
    }

    @Test
    void handlesMissingBusinessContext() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("strategy"));
    }

    @Test
    void handlesBlankBusinessContext() {
        Task task = taskWith(Map.of("businessContext", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("strategy"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
