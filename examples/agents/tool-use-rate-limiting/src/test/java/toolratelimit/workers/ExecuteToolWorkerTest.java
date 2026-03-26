package toolratelimit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteToolWorkerTest {

    private final ExecuteToolWorker worker = new ExecuteToolWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_execute_tool", worker.getTaskDefName());
    }

    @Test
    void executesToolImmediately() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello", "from", "en", "to", "fr"),
                "quotaRemaining", 50));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("executedImmediately"));
        assertEquals(180, result.getOutputData().get("executionTimeMs"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsTranslationResult() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello", "from", "en", "to", "fr"),
                "quotaRemaining", 50));
        TaskResult result = worker.execute(task);

        Map<String, Object> translation = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(translation);
        assertEquals("Bonjour, comment allez-vous aujourd'hui?", translation.get("translation"));
        assertEquals("en", translation.get("sourceLanguage"));
        assertEquals("fr", translation.get("targetLanguage"));
        assertEquals(0.98, translation.get("confidence"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("toolArgs", Map.of("text", "Hello"));
        input.put("quotaRemaining", 10);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesBlankToolName() {
        Task task = taskWith(Map.of(
                "toolName", "  ",
                "toolArgs", Map.of("text", "Hello"),
                "quotaRemaining", 10));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("executedImmediately"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
        assertEquals(true, result.getOutputData().get("executedImmediately"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello"),
                "quotaRemaining", 50));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("result"));
        assertNotNull(result.getOutputData().get("executedImmediately"));
        assertNotNull(result.getOutputData().get("executionTimeMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
