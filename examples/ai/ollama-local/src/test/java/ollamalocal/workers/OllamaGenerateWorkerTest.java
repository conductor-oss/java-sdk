package ollamalocal.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OllamaGenerateWorkerTest {

    private final OllamaGenerateWorker worker = new OllamaGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("ollama_generate", worker.getTaskDefName());
    }

    @Test
    void generatesCodeReview() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Review this JavaScript function",
                "model", "codellama:13b",
                "ollamaHost", "http://localhost:11434"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("Code Review Findings"));
        assertEquals("codellama:13b", result.getOutputData().get("model"));
    }

    @Test
    void returnsEvalMetrics() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Review this code",
                "model", "codellama:13b",
                "ollamaHost", "http://localhost:11434"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> evalMetrics = (Map<String, Object>) result.getOutputData().get("evalMetrics");
        assertNotNull(evalMetrics);
        assertNotNull(evalMetrics.get("totalDuration"));
        assertNotNull(evalMetrics.get("evalCount"));
        assertNotNull(evalMetrics.get("tokensPerSecond"));
    }

    @Test
    void failsWhenPromptMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "model", "codellama:13b"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("Prompt is required", result.getOutputData().get("error"));
    }

    @Test
    void failsWhenPromptBlank() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "   ",
                "model", "codellama:13b"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void defaultsModelWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Review this code"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("codellama:13b", result.getOutputData().get("model"));
    }

    @Test
    void responseIsDeterministic() {
        Task task1 = taskWith(new HashMap<>(Map.of("prompt", "Review code")));
        Task task2 = taskWith(new HashMap<>(Map.of("prompt", "Review code")));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("response"), result2.getOutputData().get("response"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
