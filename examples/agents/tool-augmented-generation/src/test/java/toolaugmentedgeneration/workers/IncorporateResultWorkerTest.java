package toolaugmentedgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IncorporateResultWorkerTest {

    private final IncorporateResultWorker worker = new IncorporateResultWorker();

    @Test
    void taskDefName() {
        assertEquals("tg_incorporate_result", worker.getTaskDefName());
    }

    @Test
    void producesEnrichedText() {
        Task task = taskWith(Map.of(
                "partialText", "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is",
                "toolResult", "Node.js v22.x (Jod) is the current LTS version as of 2025"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String enrichedText = (String) result.getOutputData().get("enrichedText");
        assertNotNull(enrichedText);
        assertTrue(enrichedText.contains("v22.x (Jod)"));
    }

    @Test
    void enrichedTextContainsOriginalContent() {
        Task task = taskWith(Map.of(
                "partialText", "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is",
                "toolResult", "Node.js v22.x (Jod) is the current LTS version as of 2025"));
        TaskResult result = worker.execute(task);

        String enrichedText = (String) result.getOutputData().get("enrichedText");
        assertTrue(enrichedText.contains("Ryan Dahl"));
        assertTrue(enrichedText.contains("2009"));
    }

    @Test
    void enrichedTextContainsOfficialSources() {
        Task task = taskWith(Map.of(
                "partialText", "Node.js was created by Ryan Dahl",
                "toolResult", "Node.js v22.x (Jod) is the current LTS version"));
        TaskResult result = worker.execute(task);

        String enrichedText = (String) result.getOutputData().get("enrichedText");
        assertTrue(enrichedText.contains("official sources"));
    }

    @Test
    void returnsOnlyEnrichedText() {
        Task task = taskWith(Map.of(
                "partialText", "Some text",
                "toolResult", "Some result"));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().size());
        assertNotNull(result.getOutputData().get("enrichedText"));
    }

    @Test
    void handlesEmptyPartialText() {
        Map<String, Object> input = new HashMap<>();
        input.put("partialText", "");
        input.put("toolResult", "Node.js v22.x (Jod) is the current LTS version");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enrichedText"));
    }

    @Test
    void handlesNullToolResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("partialText", "Node.js was created by Ryan Dahl");
        input.put("toolResult", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enrichedText"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("enrichedText"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
