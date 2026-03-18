package llmfallbackchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FbFormatResultWorkerTest {

    private final FbFormatResultWorker worker = new FbFormatResultWorker();

    @Test
    void taskDefName() {
        assertEquals("fb_format_result", worker.getTaskDefName());
    }

    @Test
    void selectsGeminiWhenGpt4AndClaudeFail() {
        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4Status", "failed",
                "gpt4Response", "",
                "claudeStatus", "failed",
                "claudeResponse", "",
                "geminiStatus", "success",
                "geminiResponse", "Gemini response text"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Gemini response text", result.getOutputData().get("response"));
        assertEquals("gemini-pro", result.getOutputData().get("modelUsed"));
        assertEquals(2, result.getOutputData().get("fallbacksTriggered"));
    }

    @Test
    void selectsClaudeWhenGpt4Fails() {
        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4Status", "failed",
                "gpt4Response", "",
                "claudeStatus", "success",
                "claudeResponse", "Claude response text",
                "geminiStatus", "success",
                "geminiResponse", "Gemini response text"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("Claude response text", result.getOutputData().get("response"));
        assertEquals("claude", result.getOutputData().get("modelUsed"));
        assertEquals(1, result.getOutputData().get("fallbacksTriggered"));
    }

    @Test
    void selectsGpt4WhenItSucceeds() {
        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4Status", "success",
                "gpt4Response", "GPT-4 response text",
                "claudeStatus", "failed",
                "claudeResponse", "",
                "geminiStatus", "failed",
                "geminiResponse", ""
        )));
        TaskResult result = worker.execute(task);

        assertEquals("GPT-4 response text", result.getOutputData().get("response"));
        assertEquals("gpt-4", result.getOutputData().get("modelUsed"));
        assertEquals(0, result.getOutputData().get("fallbacksTriggered"));
    }

    @Test
    void returnsNullWhenAllModelsFail() {
        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4Status", "failed",
                "gpt4Response", "",
                "claudeStatus", "failed",
                "claudeResponse", "",
                "geminiStatus", "failed",
                "geminiResponse", ""
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("response"));
        assertNull(result.getOutputData().get("modelUsed"));
        assertEquals(0, result.getOutputData().get("fallbacksTriggered"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
