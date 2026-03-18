package reactagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ActWorkerTest {

    private final ActWorker worker = new ActWorker();

    @Test
    void taskDefName() {
        assertEquals("rx_act", worker.getTaskDefName());
    }

    @Test
    void searchActionReturnsPopulationResult() {
        Task task = taskWith(Map.of(
                "thought", "I need to search for world population",
                "action", "search",
                "query", "world population 2024"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("World population is approximately 8.1 billion as of 2024",
                result.getOutputData().get("result"));
        assertEquals("search", result.getOutputData().get("source"));
    }

    @Test
    void synthesizeActionReturnsSufficientEvidence() {
        Task task = taskWith(Map.of(
                "thought", "I have enough info",
                "action", "synthesize",
                "query", "compile answer"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Sufficient evidence collected", result.getOutputData().get("result"));
        assertEquals("synthesis", result.getOutputData().get("source"));
    }

    @Test
    void unknownActionReturnsSynthesisSource() {
        Task task = taskWith(Map.of(
                "thought", "thinking",
                "action", "unknown_action",
                "query", "something"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Sufficient evidence collected", result.getOutputData().get("result"));
        assertEquals("synthesis", result.getOutputData().get("source"));
    }

    @Test
    void handlesNullAction() {
        Map<String, Object> input = new HashMap<>();
        input.put("thought", "thinking");
        input.put("action", null);
        input.put("query", "something");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Sufficient evidence collected", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullThought() {
        Map<String, Object> input = new HashMap<>();
        input.put("thought", null);
        input.put("action", "search");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("World population is approximately 8.1 billion as of 2024",
                result.getOutputData().get("result"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertNotNull(result.getOutputData().get("source"));
    }

    @Test
    void outputAlwaysContainsResultAndSource() {
        Task task = taskWith(Map.of("action", "search"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        assertTrue(result.getOutputData().containsKey("source"));
    }

    @Test
    void emptyActionTreatedAsNonSearch() {
        Map<String, Object> input = new HashMap<>();
        input.put("action", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("Sufficient evidence collected", result.getOutputData().get("result"));
        assertEquals("synthesis", result.getOutputData().get("source"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
