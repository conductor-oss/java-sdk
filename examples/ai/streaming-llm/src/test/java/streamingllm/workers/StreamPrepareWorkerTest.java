package streamingllm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StreamPrepareWorkerTest {

    private final StreamPrepareWorker worker = new StreamPrepareWorker();

    @Test
    void taskDefName() {
        assertEquals("stream_prepare", worker.getTaskDefName());
    }

    @Test
    void formatsPromptWithSystemPrefix() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Describe Conductor in one sentence",
                "model", "gpt-4"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(
                "System: You are a helpful assistant.\nUser: Describe Conductor in one sentence",
                result.getOutputData().get("formattedPrompt")
        );
    }

    @Test
    void outputContainsFormattedPromptField() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Hello",
                "model", "gpt-4"
        )));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("formattedPrompt"));
        assertTrue(result.getOutputData().get("formattedPrompt").toString().contains("Hello"));
    }

    @Test
    void formattedPromptStartsWithSystemMessage() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "test",
                "model", "gpt-4"
        )));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().get("formattedPrompt").toString()
                .startsWith("System: You are a helpful assistant."));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
