package toolaugmentedgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StartGenerationWorkerTest {

    private final StartGenerationWorker worker = new StartGenerationWorker();

    @Test
    void taskDefName() {
        assertEquals("tg_start_generation", worker.getTaskDefName());
    }

    @Test
    void producesPartialText() {
        Task task = taskWith(Map.of("prompt", "Write a brief overview of Node.js including its current version"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String partialText = (String) result.getOutputData().get("partialText");
        assertNotNull(partialText);
        assertTrue(partialText.contains("Node.js"));
        assertTrue(partialText.contains("Ryan Dahl"));
    }

    @Test
    void returnsGapType() {
        Task task = taskWith(Map.of("prompt", "Write about Node.js"));
        TaskResult result = worker.execute(task);

        assertEquals("factual_lookup", result.getOutputData().get("gapType"));
    }

    @Test
    void returnsNeedsTool() {
        Task task = taskWith(Map.of("prompt", "Write about Node.js"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("needsTool"));
    }

    @Test
    void partialTextEndsWithIncompletePhrase() {
        Task task = taskWith(Map.of("prompt", "Write about Node.js"));
        TaskResult result = worker.execute(task);

        String partialText = (String) result.getOutputData().get("partialText");
        assertTrue(partialText.endsWith("version is"));
    }

    @Test
    void handlesEmptyPrompt() {
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("partialText"));
    }

    @Test
    void handlesNullPrompt() {
        Map<String, Object> input = new HashMap<>();
        input.put("prompt", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("partialText"));
    }

    @Test
    void handlesMissingPrompt() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("partialText"));
        assertNotNull(result.getOutputData().get("gapType"));
        assertNotNull(result.getOutputData().get("needsTool"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
