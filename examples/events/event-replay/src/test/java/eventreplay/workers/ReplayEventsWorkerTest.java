package eventreplay.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReplayEventsWorkerTest {

    private final ReplayEventsWorker worker = new ReplayEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("ep_replay_events", worker.getTaskDefName());
    }

    @Test
    void replaysTwoFilteredEvents() {
        Task task = taskWith(Map.of(
                "filteredEvents", twoFilteredEvents(),
                "filteredCount", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("totalReplayed"));
        assertEquals(2, result.getOutputData().get("successCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void resultsHaveCorrectStructure() {
        Task task = taskWith(Map.of(
                "filteredEvents", twoFilteredEvents(),
                "filteredCount", 2));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(2, results.size());
        for (Map<String, Object> entry : results) {
            assertTrue(entry.containsKey("eventId"));
            assertTrue(entry.containsKey("originalType"));
            assertEquals("success", entry.get("replayStatus"));
            assertEquals("2026-01-15T10:00:00Z", entry.get("replayedAt"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesEventIdsInResults() {
        Task task = taskWith(Map.of(
                "filteredEvents", twoFilteredEvents(),
                "filteredCount", 2));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals("evt-001", results.get(0).get("eventId"));
        assertEquals("evt-005", results.get(1).get("eventId"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesOriginalTypeInResults() {
        Task task = taskWith(Map.of(
                "filteredEvents", twoFilteredEvents(),
                "filteredCount", 2));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals("order.created", results.get(0).get("originalType"));
        assertEquals("order.created", results.get(1).get("originalType"));
    }

    @Test
    void handlesEmptyFilteredEvents() {
        Task task = taskWith(Map.of(
                "filteredEvents", List.of(),
                "filteredCount", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalReplayed"));
        assertEquals(0, result.getOutputData().get("successCount"));
    }

    @Test
    void handlesNullFilteredEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("filteredEvents", null);
        input.put("filteredCount", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalReplayed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void replaysSingleEvent() {
        List<Map<String, Object>> singleEvent = List.of(
                Map.of("id", "evt-001", "type", "order.created",
                        "status", "failed"));
        Task task = taskWith(Map.of(
                "filteredEvents", singleEvent,
                "filteredCount", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("totalReplayed"));
        assertEquals(1, result.getOutputData().get("successCount"));
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(1, results.size());
    }

    private List<Map<String, Object>> twoFilteredEvents() {
        return List.of(
                Map.of("id", "evt-001", "type", "order.created", "data",
                        Map.of("orderId", "ORD-1001", "amount", 150.00),
                        "timestamp", "2026-03-07T08:15:00Z", "status", "failed"),
                Map.of("id", "evt-005", "type", "order.created", "data",
                        Map.of("orderId", "ORD-1004", "amount", 320.00),
                        "timestamp", "2026-03-07T10:45:00Z", "status", "failed")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
