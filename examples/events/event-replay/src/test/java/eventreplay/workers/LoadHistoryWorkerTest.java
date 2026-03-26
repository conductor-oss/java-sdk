package eventreplay.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadHistoryWorkerTest {

    private final LoadHistoryWorker worker = new LoadHistoryWorker();

    @Test
    void taskDefName() {
        assertEquals("ep_load_history", worker.getTaskDefName());
    }

    @Test
    void loadsFixedEvents() {
        Task task = taskWith(Map.of(
                "sourceStream", "order-events",
                "startTime", "2026-03-07T08:00:00Z",
                "endTime", "2026-03-07T12:00:00Z"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("totalLoaded"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsSixEvents() {
        Task task = taskWith(Map.of(
                "sourceStream", "order-events",
                "startTime", "2026-03-07T08:00:00Z",
                "endTime", "2026-03-07T12:00:00Z"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertNotNull(events);
        assertEquals(6, events.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void eventsHaveRequiredFields() {
        Task task = taskWith(Map.of(
                "sourceStream", "order-events",
                "startTime", "2026-03-07T08:00:00Z",
                "endTime", "2026-03-07T12:00:00Z"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        for (Map<String, Object> event : events) {
            assertTrue(event.containsKey("id"), "Event missing 'id'");
            assertTrue(event.containsKey("type"), "Event missing 'type'");
            assertTrue(event.containsKey("data"), "Event missing 'data'");
            assertTrue(event.containsKey("timestamp"), "Event missing 'timestamp'");
            assertTrue(event.containsKey("status"), "Event missing 'status'");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void firstEventIsFailedOrderCreated() {
        Task task = taskWith(Map.of(
                "sourceStream", "order-events",
                "startTime", "2026-03-07T08:00:00Z",
                "endTime", "2026-03-07T12:00:00Z"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        Map<String, Object> first = events.get(0);
        assertEquals("evt-001", first.get("id"));
        assertEquals("order.created", first.get("type"));
        assertEquals("failed", first.get("status"));
    }

    @Test
    void handlesEmptySourceStream() {
        Map<String, Object> input = new HashMap<>();
        input.put("sourceStream", "");
        input.put("startTime", "2026-03-07T08:00:00Z");
        input.put("endTime", "2026-03-07T12:00:00Z");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("totalLoaded"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("totalLoaded"));
    }

    @Test
    void handlesNullSourceStream() {
        Map<String, Object> input = new HashMap<>();
        input.put("sourceStream", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("events"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
