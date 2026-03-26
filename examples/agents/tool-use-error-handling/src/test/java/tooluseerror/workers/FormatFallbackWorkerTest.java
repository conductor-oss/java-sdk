package tooluseerror.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FormatFallbackWorkerTest {

    private final FormatFallbackWorker worker = new FormatFallbackWorker();

    @Test
    void taskDefName() {
        assertEquals("te_format_fallback", worker.getTaskDefName());
    }

    @Test
    void formatsResultFromFallbackTool() {
        Map<String, Object> inputResult = Map.of("location", "San Francisco, CA");
        Task task = taskWith(Map.of("result", inputResult, "toolName", "openstreetmap_nominatim", "source", "fallback"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Location: San Francisco, CA (37.7899, -122.4194)", result.getOutputData().get("formatted"));
        assertEquals("fallback", result.getOutputData().get("source"));
        assertEquals("openstreetmap_nominatim", result.getOutputData().get("toolName"));
        assertEquals(true, result.getOutputData().get("reliable"));
        assertEquals(true, result.getOutputData().get("fallbackUsed"));
    }

    @Test
    void alwaysSetSourceToFallback() {
        Task task = taskWith(Map.of("result", "data", "toolName", "tool_b", "source", "primary"));
        TaskResult result = worker.execute(task);

        assertEquals("fallback", result.getOutputData().get("source"));
    }

    @Test
    void alwaysSetFallbackUsedTrue() {
        Task task = taskWith(Map.of("result", "data", "toolName", "tool_b"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("fallbackUsed"));
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
        Task task = taskWith(Map.of("result", "data", "toolName", "tool_b"));
        TaskResult result = worker.execute(task);

        assertEquals("fallback", result.getOutputData().get("source"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reliable"));
        assertEquals(true, result.getOutputData().get("fallbackUsed"));
    }

    @Test
    void returnsFormattedString() {
        Task task = taskWith(Map.of("result", Map.of("location", "test"), "toolName", "nominatim"));
        TaskResult result = worker.execute(task);

        assertEquals("Location: San Francisco, CA (37.7899, -122.4194)", result.getOutputData().get("formatted"));
    }

    @Test
    void handlesBlankToolName() {
        Task task = taskWith(Map.of("result", "data", "toolName", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown_tool", result.getOutputData().get("toolName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
