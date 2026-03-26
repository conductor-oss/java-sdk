package eventreplaytesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadEventsWorkerTest {

    private final LoadEventsWorker worker = new LoadEventsWorker();

    @Test
    void taskDefName() {
        assertEquals("rt_load_events", worker.getTaskDefName());
    }

    @Test
    void loadsThreeEvents() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-001",
                "eventSource", "recorded-events-db"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, String>> events = (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals(3, events.size());
    }

    @Test
    void countMatchesEventsSize() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-002",
                "eventSource", "test-source"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void firstEventIsOrderCreated() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-003",
                "eventSource", "test-source"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events = (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals("rec-1", events.get(0).get("id"));
        assertEquals("order.created", events.get(0).get("type"));
        assertEquals("processed", events.get(0).get("expected"));
    }

    @Test
    void secondEventIsPaymentReceived() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-004",
                "eventSource", "test-source"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events = (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals("rec-2", events.get(1).get("id"));
        assertEquals("payment.received", events.get(1).get("type"));
    }

    @Test
    void thirdEventIsShippingLabel() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-005",
                "eventSource", "test-source"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events = (List<Map<String, String>>) result.getOutputData().get("events");
        assertEquals("rec-3", events.get(2).get("id"));
        assertEquals("shipping.label", events.get(2).get("type"));
    }

    @Test
    void handlesNullTestSuiteId() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuiteId", null);
        input.put("eventSource", "test-source");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("events"));
    }

    @Test
    void handlesNullEventSource() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuiteId", "suite-006");
        input.put("eventSource", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("events"));
        assertEquals(3, result.getOutputData().get("count"));
    }

    @Test
    void allEventsExpectProcessed() {
        Task task = taskWith(Map.of(
                "testSuiteId", "suite-007",
                "eventSource", "test-source"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> events = (List<Map<String, String>>) result.getOutputData().get("events");
        for (Map<String, String> event : events) {
            assertEquals("processed", event.get("expected"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
