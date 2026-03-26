package logaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectLogsTest {

    private final CollectLogs worker = new CollectLogs();

    @Test
    void taskDefName() {
        assertEquals("la_collect_logs", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("sources", List.of("api-gateway"), "timeRange", "last-1h", "logLevel", "INFO"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void returnsRawLogCount() {
        Task task = taskWith(Map.of("sources", List.of("svc1", "svc2"), "timeRange", "last-1h"));
        TaskResult result = worker.execute(task);
        assertEquals(15000, result.getOutputData().get("rawLogCount"));
    }

    @Test
    void returnsFormat() {
        Task task = taskWith(Map.of("sources", List.of("svc1"), "timeRange", "last-5m"));
        TaskResult result = worker.execute(task);
        assertEquals("mixed", result.getOutputData().get("format"));
    }

    @Test
    void returnsCollectedTimestamp() {
        Task task = taskWith(Map.of("sources", List.of("svc1"), "timeRange", "last-1h"));
        TaskResult result = worker.execute(task);
        assertEquals("2026-03-08T06:00:00Z", result.getOutputData().get("collectedAt"));
    }

    @Test
    void passesThroughSources() {
        List<String> sources = List.of("api-gateway", "auth-service");
        Task task = taskWith(Map.of("sources", sources, "timeRange", "last-1h"));
        TaskResult result = worker.execute(task);
        assertEquals(sources, result.getOutputData().get("sources"));
    }

    @Test
    void handlesNullSources() {
        Task task = taskWith(Map.of("timeRange", "last-1h"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(15000, result.getOutputData().get("rawLogCount"));
    }

    @Test
    void handlesNullTimeRange() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
