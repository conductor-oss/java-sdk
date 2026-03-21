package realtimeanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProcessStreamWorkerTest {

    private final ProcessStreamWorker worker = new ProcessStreamWorker();

    @Test
    void taskDefName() {
        assertEquals("ry_process_stream", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void processesEvents() {
        List<Map<String, Object>> events = List.of(
                Map.of("eventId", "E1", "type", "page_view", "userId", "U1", "latency", 100),
                Map.of("eventId", "E2", "type", "error", "userId", "U2", "latency", 0));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("processedCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void flagsAnomalies() {
        List<Map<String, Object>> events = List.of(
                Map.of("eventId", "E1", "type", "page_view", "userId", "U1", "latency", 600),
                Map.of("eventId", "E2", "type", "page_view", "userId", "U2", "latency", 100));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("windowMetrics");
        assertEquals(1, metrics.get("anomalyCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void countsErrors() {
        List<Map<String, Object>> events = List.of(
                Map.of("eventId", "E1", "type", "error", "userId", "U1", "latency", 0),
                Map.of("eventId", "E2", "type", "error", "userId", "U2", "latency", 0));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        Map<String, Object> metrics = (Map<String, Object>) result.getOutputData().get("windowMetrics");
        assertEquals(2, metrics.get("errorCount"));
    }

    @Test
    void handlesEmptyEvents() {
        Task task = taskWith(Map.of("events", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("processedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
