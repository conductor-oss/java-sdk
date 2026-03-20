package streamingllm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamCollectChunksWorkerTest {

    private final StreamCollectChunksWorker worker = new StreamCollectChunksWorker();

    @Test
    void taskDefName() {
        assertEquals("stream_collect_chunks", worker.getTaskDefName());
    }

    @Test
    void assemblesChunksIntoFullResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "System: You are a helpful assistant.\nUser: Describe Conductor",
                "model", "gpt-4",
                "maxTokens", 200
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        // In simulated mode, verify the fixed assembled response
        if (System.getenv("CONDUCTOR_OPENAI_API_KEY") == null || System.getenv("CONDUCTOR_OPENAI_API_KEY").isBlank()) {
            assertEquals(
                    "Conductor orchestrates complex workflows with built-in durability, "
                            + "retry logic, and full observability \u2014 making it ideal for production AI pipelines.",
                    result.getOutputData().get("fullResponse")
            );
        } else {
            // In live mode, just verify we got a non-empty response
            String fullResponse = (String) result.getOutputData().get("fullResponse");
            assertNotNull(fullResponse);
            assertFalse(fullResponse.isEmpty());
        }
    }

    @Test
    void returnsCorrectChunkCount() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "test",
                "model", "gpt-4",
                "maxTokens", 200
        )));
        TaskResult result = worker.execute(task);

        // In simulated mode, chunk count is fixed at 11
        if (System.getenv("CONDUCTOR_OPENAI_API_KEY") == null || System.getenv("CONDUCTOR_OPENAI_API_KEY").isBlank()) {
            assertEquals(11, result.getOutputData().get("chunkCount"));
        } else {
            assertTrue(((Number) result.getOutputData().get("chunkCount")).intValue() > 0);
        }
    }

    @Test
    void returnsStreamMetrics() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "test",
                "model", "gpt-4",
                "maxTokens", 200
        )));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("streamDurationMs"));
        assertNotNull(result.getOutputData().get("tokensPerSecond"));

        // In simulated mode, verify fixed metrics
        if (System.getenv("CONDUCTOR_OPENAI_API_KEY") == null || System.getenv("CONDUCTOR_OPENAI_API_KEY").isBlank()) {
            assertEquals(1240, result.getOutputData().get("streamDurationMs"));
            assertEquals(45, result.getOutputData().get("tokensPerSecond"));
        }
    }

    @Test
    void fullResponseIsTrimmed() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "test",
                "model", "gpt-4",
                "maxTokens", 200
        )));
        TaskResult result = worker.execute(task);

        String fullResponse = (String) result.getOutputData().get("fullResponse");
        assertEquals(fullResponse.trim(), fullResponse);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
