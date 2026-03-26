package logprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseEntriesWorkerTest {

    private final ParseEntriesWorker worker = new ParseEntriesWorker();

    @Test
    void taskDefName() {
        assertEquals("lp_parse_entries", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void parsesLogEntries() {
        List<Map<String, Object>> rawLogs = List.of(
                Map.of("ts", "2024-03-15T10:00:01Z", "level", "INFO", "service", "api", "msg", "Request received"),
                Map.of("ts", "2024-03-15T10:00:02Z", "level", "ERROR", "service", "auth", "msg", "Token expired"));
        Task task = taskWith(Map.of("rawLogs", rawLogs));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("parsedCount"));

        List<Map<String, Object>> entries = (List<Map<String, Object>>) result.getOutputData().get("entries");
        assertEquals("Request received", entries.get(0).get("message"));
        assertEquals(false, entries.get(0).get("isError"));
        assertEquals(true, entries.get(1).get("isError"));
    }

    @Test
    void countsErrorsAndWarnings() {
        List<Map<String, Object>> rawLogs = List.of(
                Map.of("ts", "t1", "level", "ERROR", "service", "s1", "msg", "err1"),
                Map.of("ts", "t2", "level", "WARN", "service", "s1", "msg", "warn1"),
                Map.of("ts", "t3", "level", "ERROR", "service", "s2", "msg", "err2"));
        Task task = taskWith(Map.of("rawLogs", rawLogs));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("errorCount"));
        assertEquals(1, result.getOutputData().get("warnCount"));
    }

    @Test
    void handlesNullRawLogs() {
        Map<String, Object> input = new HashMap<>();
        input.put("rawLogs", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("parsedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
