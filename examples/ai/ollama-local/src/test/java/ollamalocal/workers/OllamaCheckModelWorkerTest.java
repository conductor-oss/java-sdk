package ollamalocal.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OllamaCheckModelWorkerTest {

    private final OllamaCheckModelWorker worker = new OllamaCheckModelWorker();

    @Test
    void taskDefName() {
        assertEquals("ollama_check_model", worker.getTaskDefName());
    }

    @Test
    void checksModelAvailability() {
        Task task = taskWith(new HashMap<>(Map.of(
                "model", "codellama:13b",
                "ollamaHost", "http://localhost:11434"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("codellama:13b", result.getOutputData().get("resolvedModel"));
        assertEquals(true, result.getOutputData().get("available"));
    }

    @Test
    void failsWhenModelMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "ollamaHost", "http://localhost:11434"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("Model name is required", result.getOutputData().get("error"));
    }

    @Test
    void failsWhenModelBlank() {
        Task task = taskWith(new HashMap<>(Map.of(
                "model", "   ",
                "ollamaHost", "http://localhost:11434"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void defaultsOllamaHostWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "model", "llama2:7b"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("llama2:7b", result.getOutputData().get("resolvedModel"));
        assertEquals(true, result.getOutputData().get("available"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
