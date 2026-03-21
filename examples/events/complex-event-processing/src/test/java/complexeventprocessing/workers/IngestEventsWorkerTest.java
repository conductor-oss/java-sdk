package complexeventprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IngestEventsWorkerTest {

    private final IngestEventsWorker worker = new IngestEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("cp_ingest_events", worker.getTaskDefName());
    }

    @Test
    void ingestsMultipleEvents() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000),
                Map.of("type", "browse", "ts", 1500),
                Map.of("type", "purchase", "ts", 8000));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(events, result.getOutputData().get("events"));
        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void ingestsSingleEvent() {
        List<Map<String, Object>> events = List.of(Map.of("type", "login", "ts", 1000));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals(1, result.getOutputData().get("count"));
        assertEquals(events, result.getOutputData().get("events"));
    }

    @Test
    void ingestsEmptyList() {
        List<Map<String, Object>> events = List.of();
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
        assertNotNull(result.getOutputData().get("events"));
    }

    @Test
    void handlesMissingEventsKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("count"));
    }

    @Test
    void preservesEventData() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "login", "ts", 1000, "userId", "U-50"));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> outputEvents = (List<Map<String, Object>>) result.getOutputData().get("events");
        assertEquals("U-50", outputEvents.get(0).get("userId"));
    }

    @Test
    void countMatchesListSize() {
        List<Map<String, Object>> events = List.of(
                Map.of("type", "a", "ts", 100),
                Map.of("type", "b", "ts", 200),
                Map.of("type", "c", "ts", 300),
                Map.of("type", "d", "ts", 400),
                Map.of("type", "e", "ts", 500));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("count"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
