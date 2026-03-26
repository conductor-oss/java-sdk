package multimodelcompare.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McCallGeminiWorkerTest {

    private final McCallGeminiWorker worker = new McCallGeminiWorker();

    @Test
    void taskDefName() {
        assertEquals("mc_call_gemini", worker.getTaskDefName());
    }

    @Test
    void returnsGeminiResponse() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "What is Conductor?")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("gemini-pro", result.getOutputData().get("model"));
        assertEquals("Conductor orchestrates workflows across services with built-in retry, human-in-the-loop, and monitoring.",
                result.getOutputData().get("response"));
        assertEquals(750, result.getOutputData().get("latencyMs"));
        assertEquals(78, result.getOutputData().get("tokens"));
        assertEquals(0.0001, result.getOutputData().get("cost"));
        assertEquals(8.5, result.getOutputData().get("quality"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "test")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("model"));
        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("latencyMs"));
        assertTrue(result.getOutputData().containsKey("tokens"));
        assertTrue(result.getOutputData().containsKey("cost"));
        assertTrue(result.getOutputData().containsKey("quality"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
