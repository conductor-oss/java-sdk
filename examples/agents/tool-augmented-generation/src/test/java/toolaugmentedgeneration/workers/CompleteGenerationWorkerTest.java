package toolaugmentedgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompleteGenerationWorkerTest {

    private final CompleteGenerationWorker worker = new CompleteGenerationWorker();

    @Test
    void taskDefName() {
        assertEquals("tg_complete_generation", worker.getTaskDefName());
    }

    @Test
    void producesFinalText() {
        Task task = taskWith(Map.of(
                "enrichedText", "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is v22.x (Jod), as confirmed by official sources.",
                "prompt", "Write a brief overview of Node.js including its current version"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String finalText = (String) result.getOutputData().get("finalText");
        assertNotNull(finalText);
        assertTrue(finalText.contains("event-driven"));
        assertTrue(finalText.contains("non-blocking I/O"));
    }

    @Test
    void finalTextContainsEnrichedContent() {
        Task task = taskWith(Map.of(
                "enrichedText", "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is v22.x (Jod), as confirmed by official sources.",
                "prompt", "Write about Node.js"));
        TaskResult result = worker.execute(task);

        String finalText = (String) result.getOutputData().get("finalText");
        assertTrue(finalText.contains("Ryan Dahl"));
        assertTrue(finalText.contains("v22.x (Jod)"));
    }

    @Test
    void returnsTotalTokens() {
        Task task = taskWith(Map.of(
                "enrichedText", "Some enriched text",
                "prompt", "Write about Node.js"));
        TaskResult result = worker.execute(task);

        assertEquals(87, result.getOutputData().get("totalTokens"));
    }

    @Test
    void finalTextMentionsScalableApplications() {
        Task task = taskWith(Map.of(
                "enrichedText", "Some enriched text",
                "prompt", "Write about Node.js"));
        TaskResult result = worker.execute(task);

        String finalText = (String) result.getOutputData().get("finalText");
        assertTrue(finalText.contains("scalable server-side applications"));
        assertTrue(finalText.contains("APIs"));
    }

    @Test
    void handlesEmptyEnrichedText() {
        Map<String, Object> input = new HashMap<>();
        input.put("enrichedText", "");
        input.put("prompt", "Write about Node.js");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("finalText"));
    }

    @Test
    void handlesNullPrompt() {
        Map<String, Object> input = new HashMap<>();
        input.put("enrichedText", "Some enriched text");
        input.put("prompt", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("finalText"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("finalText"));
        assertNotNull(result.getOutputData().get("totalTokens"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
