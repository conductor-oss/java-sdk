package logaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseLogsTest {

    private final ParseLogs worker = new ParseLogs();

    @Test
    void taskDefName() {
        assertEquals("la_parse_logs", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("rawLogCount", 15000, "format", "mixed"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsParsedCount() {
        Task task = taskWith(Map.of("rawLogCount", 15000, "format", "mixed"));
        TaskResult result = worker.execute(task);
        assertEquals(14800, result.getOutputData().get("parsedCount"));
    }

    @Test
    void returnsParseErrors() {
        Task task = taskWith(Map.of("rawLogCount", 15000, "format", "mixed"));
        TaskResult result = worker.execute(task);
        assertEquals(200, result.getOutputData().get("parseErrors"));
    }

    @Test
    void returnsStructuredLogsFormat() {
        Task task = taskWith(Map.of("rawLogCount", 15000, "format", "json"));
        TaskResult result = worker.execute(task);
        assertEquals("json", result.getOutputData().get("structuredLogs"));
    }

    @Test
    void returnsAvgParseTime() {
        Task task = taskWith(Map.of("rawLogCount", 15000, "format", "mixed"));
        TaskResult result = worker.execute(task);
        assertEquals(0.2, result.getOutputData().get("avgParseTimeMs"));
    }

    @Test
    void handlesStringRawLogCount() {
        Task task = taskWith(Map.of("rawLogCount", "15000", "format", "mixed"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(14800, result.getOutputData().get("parsedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
