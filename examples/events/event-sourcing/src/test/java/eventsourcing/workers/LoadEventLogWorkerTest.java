package eventsourcing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LoadEventLogWorkerTest {

    private final LoadEventLogWorker worker = new LoadEventLogWorker();

    @Test
    void taskDefName() {
        assertEquals("ev_load_event_log", worker.getTaskDefName());
    }

    @Test
    void returnsFixedEventLog() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertNotNull(events);
        assertEquals(4, events.size());
    }

    @Test
    void returnsEventCount() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("eventCount"));
    }

    @Test
    void returnsNextSequence() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("nextSequence"));
    }

    @Test
    void firstEventIsAccountCreated() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        Map<String, Object> first = events.get(0);
        assertEquals(1, first.get("sequence"));
        assertEquals("AccountCreated", first.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) first.get("data");
        assertEquals("Alice", data.get("owner"));
        assertEquals(0, data.get("initialBalance"));
    }

    @Test
    void eventsContainFundsDepositedAndWithdrawn() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        assertEquals("FundsDeposited", events.get(1).get("type"));
        assertEquals("FundsWithdrawn", events.get(2).get("type"));
        assertEquals("FundsDeposited", events.get(3).get("type"));
    }

    @Test
    void eventsHaveSequentialNumbers() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) result.getOutputData().get("events");
        for (int i = 0; i < events.size(); i++) {
            assertEquals(i + 1, events.get(i).get("sequence"));
        }
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("events"));
    }

    @Test
    void handlesNullAggregateId() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregateId", null);
        input.put("aggregateType", "BankAccount");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("eventCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
