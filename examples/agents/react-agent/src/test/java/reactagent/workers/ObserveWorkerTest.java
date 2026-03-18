package reactagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObserveWorkerTest {

    private final ObserveWorker worker = new ObserveWorker();

    @Test
    void taskDefName() {
        assertEquals("rx_observe", worker.getTaskDefName());
    }

    @Test
    void observesActionResult() {
        Task task = taskWith(Map.of(
                "actionResult", "World population is approximately 8.1 billion as of 2024",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String observation = (String) result.getOutputData().get("observation");
        assertTrue(observation.contains("Iteration 1"));
        assertTrue(observation.contains("8.1 billion"));
    }

    @Test
    void alwaysMarksUseful() {
        Task task = taskWith(Map.of("actionResult", "some result", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("useful"));
    }

    @Test
    void includesIterationInObservation() {
        Task task = taskWith(Map.of("actionResult", "test result", "iteration", 3));
        TaskResult result = worker.execute(task);

        String observation = (String) result.getOutputData().get("observation");
        assertTrue(observation.startsWith("Iteration 3:"));
    }

    @Test
    void handlesNullActionResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("actionResult", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("observation"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("actionResult", "some result"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String observation = (String) result.getOutputData().get("observation");
        assertTrue(observation.startsWith("Iteration 1:"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("observation"));
        assertTrue(result.getOutputData().containsKey("useful"));
    }

    @Test
    void observationContainsFullActionResult() {
        String actionResult = "Sufficient evidence collected";
        Task task = taskWith(Map.of("actionResult", actionResult, "iteration", 2));
        TaskResult result = worker.execute(task);

        String observation = (String) result.getOutputData().get("observation");
        assertTrue(observation.contains(actionResult));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
