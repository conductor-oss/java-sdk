package eventaggregation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectEventsWorkerTest {

    private final CollectEventsWorker worker = new CollectEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("eg_collect_events", worker.getTaskDefName());
    }

    @Test
    void collectsFixedEvents() {
        Task task = taskWith(Map.of(
                "windowId", "win-fixed-001",
                "windowDurationSec", 60,
                "eventSource", "transaction-stream"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("eventCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsCorrectEventList() {
        Task task = taskWith(Map.of(
                "windowId", "win-fixed-001",
                "windowDurationSec", 60,
                "eventSource", "transaction-stream"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertNotNull(events);
        assertEquals(6, events.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void firstEventIsPurchaseWidgetA() {
        Task task = taskWith(Map.of(
                "windowId", "win-fixed-001",
                "windowDurationSec", 60,
                "eventSource", "transaction-stream"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        Map<String, Object> first = events.get(0);
        assertEquals("e1", first.get("id"));
        assertEquals("purchase", first.get("type"));
        assertEquals(49.99, first.get("amount"));
        assertEquals("Widget A", first.get("product"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void thirdEventIsRefund() {
        Task task = taskWith(Map.of(
                "windowId", "win-fixed-001",
                "windowDurationSec", 60,
                "eventSource", "transaction-stream"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        Map<String, Object> third = events.get(2);
        assertEquals("e3", third.get("id"));
        assertEquals("refund", third.get("type"));
        assertEquals(-25.00, third.get("amount"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("eventCount"));
        assertNotNull(result.getOutputData().get("events"));
    }

    @Test
    void handlesNullWindowId() {
        Map<String, Object> input = new HashMap<>();
        input.put("windowId", null);
        input.put("windowDurationSec", 60);
        input.put("eventSource", "transaction-stream");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("eventCount"));
    }

    @Test
    void handlesStringDuration() {
        Task task = taskWith(Map.of(
                "windowId", "win-test",
                "windowDurationSec", "120",
                "eventSource", "test-stream"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(6, result.getOutputData().get("eventCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sixthEventIsPurchaseWidgetA() {
        Task task = taskWith(Map.of(
                "windowId", "win-fixed-001",
                "windowDurationSec", 60,
                "eventSource", "transaction-stream"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        Map<String, Object> sixth = events.get(5);
        assertEquals("e6", sixth.get("id"));
        assertEquals("purchase", sixth.get("type"));
        assertEquals(34.99, sixth.get("amount"));
        assertEquals("Widget A", sixth.get("product"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
