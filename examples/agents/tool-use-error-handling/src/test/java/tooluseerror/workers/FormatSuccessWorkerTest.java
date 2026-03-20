package tooluseerror.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatSuccessWorkerTest {

    private final FormatSuccessWorker worker = new FormatSuccessWorker();

    @Test
    void taskDefName() {
        assertEquals("te_format_success", worker.getTaskDefName());
    }

    @Test
    void formatsResultFromPrimaryTool() {
        Map<String, Object> inputResult = Map.of("location", "San Francisco", "lat", 37.7899);
        Task task = taskWith(Map.of("result", inputResult, "toolName", "google_geocoding_api", "source", "primary"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(inputResult, result.getOutputData().get("formatted"));
        assertEquals("primary", result.getOutputData().get("source"));
        assertEquals("google_geocoding_api", result.getOutputData().get("toolName"));
        assertEquals(true, result.getOutputData().get("reliable"));
    }

    @Test
    void alwaysSetSourceToPrimary() {
        Task task = taskWith(Map.of("result", "data", "toolName", "tool_a", "source", "other"));
        TaskResult result = worker.execute(task);

        assertEquals("primary", result.getOutputData().get("source"));
    }

    @Test
    void handlesNullResult() {
        Map<String, Object> input = new HashMap<>();
        input.put("result", null);
        input.put("toolName", "some_tool");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("formatted"));
    }

    @Test
    void handlesNullToolName() {
        Map<String, Object> input = new HashMap<>();
        input.put("result", "data");
        input.put("toolName", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown_tool", result.getOutputData().get("toolName"));
    }

    @Test
    void handlesMissingSource() {
        Task task = taskWith(Map.of("result", "data", "toolName", "tool_a"));
        TaskResult result = worker.execute(task);

        assertEquals("primary", result.getOutputData().get("source"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reliable"));
    }

    @Test
    void handlesStringResult() {
        Task task = taskWith(Map.of("result", "plain text result", "toolName", "text_tool"));
        TaskResult result = worker.execute(task);

        assertEquals("plain text result", result.getOutputData().get("formatted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
