package ollamalocal.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OllamaPostProcessWorkerTest {

    private final OllamaPostProcessWorker worker = new OllamaPostProcessWorker();

    @Test
    void taskDefName() {
        assertEquals("ollama_post_process", worker.getTaskDefName());
    }

    @Test
    void postProcessesResponse() {
        Task task = taskWith(new HashMap<>(Map.of(
                "response", "Code Review Findings: missing error handling"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Code Review Findings: missing error handling", result.getOutputData().get("review"));
    }

    @Test
    void failsWhenResponseMissing() {
        Task task = taskWith(new HashMap<>(Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertEquals("Response is required", result.getOutputData().get("error"));
    }

    @Test
    void failsWhenResponseBlank() {
        Task task = taskWith(new HashMap<>(Map.of(
                "response", "   "
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
    }

    @Test
    void reviewMatchesInputResponse() {
        String input = "1. Fix null check\n2. Add tests";
        Task task = taskWith(new HashMap<>(Map.of("response", input)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(input, result.getOutputData().get("review"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
