package streamingllm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamPostProcessWorkerTest {

    private final StreamPostProcessWorker worker = new StreamPostProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("stream_post_process", worker.getTaskDefName());
    }

    @Test
    void countsWordsCorrectly() {
        Task task = taskWith(new HashMap<>(Map.of(
                "fullResponse", "Conductor orchestrates complex workflows with built-in durability, "
                        + "retry logic, and full observability \u2014 making it ideal for production AI pipelines.",
                "chunkCount", 11,
                "streamDurationMs", 1240
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(20, result.getOutputData().get("wordCount"));
    }

    @Test
    void setsProcessedFlag() {
        Task task = taskWith(new HashMap<>(Map.of(
                "fullResponse", "hello world",
                "chunkCount", 1,
                "streamDurationMs", 100
        )));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void singleWordResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "fullResponse", "hello",
                "chunkCount", 1,
                "streamDurationMs", 50
        )));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("wordCount"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void multiWordResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "fullResponse", "one two three four five",
                "chunkCount", 5,
                "streamDurationMs", 500
        )));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("wordCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
