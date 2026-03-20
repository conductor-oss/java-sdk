package eventreplay.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterEventsWorkerTest {

    private final FilterEventsWorker worker = new FilterEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("ep_filter_events", worker.getTaskDefName());
    }

    @Test
    void filtersFailedOrderCreatedEvents() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "totalLoaded", 6,
                "filterCriteria", Map.of("status", "failed", "eventType", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("filteredCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void filteredEventsMatchCriteria() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "totalLoaded", 6,
                "filterCriteria", Map.of("status", "failed", "eventType", "order.created")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> filtered =
                (List<Map<String, Object>>) result.getOutputData().get("filteredEvents");
        assertNotNull(filtered);
        assertEquals(2, filtered.size());
        for (Map<String, Object> event : filtered) {
            assertEquals("failed", event.get("status"));
            assertEquals("order.created", event.get("type"));
        }
    }

    @Test
    void filtersByStatusOnly() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "totalLoaded", 6,
                "filterCriteria", Map.of("status", "failed")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("filteredCount"));
    }

    @Test
    void filtersByEventTypeOnly() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "totalLoaded", 6,
                "filterCriteria", Map.of("eventType", "order.created")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("filteredCount"));
    }

    @Test
    void returnsAllEventsWithNoCriteria() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", sampleEvents());
        input.put("totalLoaded", 6);
        input.put("filterCriteria", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("filteredCount"));
    }

    @Test
    void handlesEmptyEventsList() {
        Task task = taskWith(Map.of(
                "events", List.of(),
                "totalLoaded", 0,
                "filterCriteria", Map.of("status", "failed")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("filteredCount"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("totalLoaded", 0);
        input.put("filterCriteria", Map.of("status", "failed"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("filteredCount"));
    }

    @Test
    void filtersSuccessEvents() {
        Task task = taskWith(Map.of(
                "events", sampleEvents(),
                "totalLoaded", 6,
                "filterCriteria", Map.of("status", "success")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("filteredCount"));
    }

    private List<Map<String, Object>> sampleEvents() {
        return List.of(
                Map.of("id", "evt-001", "type", "order.created", "data",
                        Map.of("orderId", "ORD-1001", "amount", 150.00),
                        "timestamp", "2026-03-07T08:15:00Z", "status", "failed"),
                Map.of("id", "evt-002", "type", "order.updated", "data",
                        Map.of("orderId", "ORD-1002", "amount", 75.50),
                        "timestamp", "2026-03-07T09:00:00Z", "status", "success"),
                Map.of("id", "evt-003", "type", "payment.processed", "data",
                        Map.of("paymentId", "PAY-2001", "amount", 150.00),
                        "timestamp", "2026-03-07T09:30:00Z", "status", "failed"),
                Map.of("id", "evt-004", "type", "order.created", "data",
                        Map.of("orderId", "ORD-1003", "amount", 200.00),
                        "timestamp", "2026-03-07T10:00:00Z", "status", "success"),
                Map.of("id", "evt-005", "type", "order.created", "data",
                        Map.of("orderId", "ORD-1004", "amount", 320.00),
                        "timestamp", "2026-03-07T10:45:00Z", "status", "failed"),
                Map.of("id", "evt-006", "type", "payment.processed", "data",
                        Map.of("paymentId", "PAY-2002", "amount", 200.00),
                        "timestamp", "2026-03-07T11:30:00Z", "status", "success")
        );
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
