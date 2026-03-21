package tooluselogging.workers;

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
        assertEquals("tl_execute_tool", worker.getTaskDefName());
    }

    @Test
    void returnsSentimentResult() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", Map.of("text", "I am thrilled!"),
                "requestId", "req-fixed-abc123"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(sentimentResult);
        assertEquals("positive", sentimentResult.get("sentiment"));
        assertEquals(0.94, sentimentResult.get("confidence"));
    }

    @Test
    void resultContainsEmotions() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", Map.of("text", "Great product!"),
                "requestId", "req-1"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentResult = (Map<String, Object>) result.getOutputData().get("result");
        @SuppressWarnings("unchecked")
        Map<String, Object> emotions = (Map<String, Object>) sentimentResult.get("emotions");
        assertNotNull(emotions);
        assertEquals(0.72, emotions.get("joy"));
        assertEquals(0.65, emotions.get("trust"));
        assertEquals(0.58, emotions.get("anticipation"));
        assertEquals(0.12, emotions.get("surprise"));
        assertEquals(0.02, emotions.get("anger"));
        assertEquals(0.05, emotions.get("sadness"));
    }

    @Test
    void returnsExecutionTimeAndStatus() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", Map.of("text", "test"),
                "requestId", "req-1"));
        TaskResult result = worker.execute(task);

        assertEquals(187, result.getOutputData().get("executionTimeMs"));
        assertEquals("success", result.getOutputData().get("toolStatus"));
    }

    @Test
    void resultContainsLanguageAndWordCount() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", Map.of("text", "hello world"),
                "requestId", "req-1"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("en", sentimentResult.get("language"));
        assertEquals(15, sentimentResult.get("wordCount"));
    }

    @Test
    void resultContainsInputText() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", Map.of("text", "I am thrilled!"),
                "requestId", "req-1"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("I am thrilled!", sentimentResult.get("text"));
    }

    @Test
    void handlesNullToolArgs() {
        Map<String, Object> input = new HashMap<>();
        input.put("toolName", "sentiment_analysis");
        input.put("toolArgs", null);
        input.put("requestId", "req-1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("", sentimentResult.get("text"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void handlesNonMapToolArgs() {
        Task task = taskWith(Map.of(
                "toolName", "sentiment_analysis",
                "toolArgs", "not-a-map",
                "requestId", "req-1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> sentimentResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("", sentimentResult.get("text"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
