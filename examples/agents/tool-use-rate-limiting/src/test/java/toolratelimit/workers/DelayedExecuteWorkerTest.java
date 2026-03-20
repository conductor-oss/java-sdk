package toolratelimit.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DelayedExecuteWorkerTest {

    private final DelayedExecuteWorker worker = new DelayedExecuteWorker();

    @Test
    void taskDefName() {
        assertEquals("rl_delayed_execute", worker.getTaskDefName());
    }

    @Test
    void executesDelayedRequest() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello", "from", "en", "to", "fr"),
                "queueId", "q-fixed-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("executedImmediately"));
        assertEquals(5000, result.getOutputData().get("delayedByMs"));
        assertEquals("q-fixed-001", result.getOutputData().get("queueId"));
        assertEquals(195, result.getOutputData().get("executionTimeMs"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsTranslationResult() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello"),
                "queueId", "q-fixed-001"));
        TaskResult result = worker.execute(task);

        Map<String, Object> translation = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(translation);
        assertEquals("Bonjour, comment allez-vous aujourd'hui?", translation.get("translation"));
        assertEquals("en", translation.get("sourceLanguage"));
        assertEquals("fr", translation.get("targetLanguage"));
        assertEquals(0.98, translation.get("confidence"));
    }

    @Test
    void preservesQueueId() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello"),
                "queueId", "q-custom-999"));
        TaskResult result = worker.execute(task);

        assertEquals("q-custom-999", result.getOutputData().get("queueId"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", null);
        input.put("toolArgs", Map.of("text", "Hello"));
        input.put("queueId", "q-fixed-001");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNullQueueId() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "translation_api");
        input.put("queueId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("q-unknown", result.getOutputData().get("queueId"));
    }

    @Test
    void handlesBlankQueueId() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "queueId", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("q-unknown", result.getOutputData().get("queueId"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(false, result.getOutputData().get("executedImmediately"));
        assertEquals(5000, result.getOutputData().get("delayedByMs"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of(
                "toolName", "translation_api",
                "toolArgs", Map.of("text", "Hello"),
                "queueId", "q-fixed-001"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("result"));
        assertNotNull(result.getOutputData().get("executedImmediately"));
        assertNotNull(result.getOutputData().get("delayedByMs"));
        assertNotNull(result.getOutputData().get("queueId"));
        assertNotNull(result.getOutputData().get("executionTimeMs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
