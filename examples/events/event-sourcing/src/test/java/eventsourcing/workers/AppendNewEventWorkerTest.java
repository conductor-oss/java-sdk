package eventsourcing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AppendNewEventWorkerTest {

    private final AppendNewEventWorker worker = new AppendNewEventWorker();

    @Test
    void taskDefName() {
        assertEquals("ev_append_new_event", worker.getTaskDefName());
    }

    @Test
    void appendsEventToExistingList() {
        List<Map<String, Object>> existing = new ArrayList<>();
        existing.add(Map.of("sequence", 1, "type", "AccountCreated"));
        existing.add(Map.of("sequence", 2, "type", "FundsDeposited"));

        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "existingEvents", existing,
                "newEvent", Map.of("type", "FundsDeposited", "data", Map.of("amount", 250, "source", "check_deposit")),
                "nextSequence", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allEvents =
                (List<Map<String, Object>>) result.getOutputData().get("allEvents");
        assertEquals(3, allEvents.size());
    }

    @Test
    void appendedEventHasCorrectSequence() {
        List<Map<String, Object>> existing = new ArrayList<>();
        existing.add(Map.of("sequence", 1, "type", "AccountCreated"));

        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "existingEvents", existing,
                "newEvent", Map.of("type", "FundsDeposited", "data", Map.of("amount", 100)),
                "nextSequence", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> appended =
                (Map<String, Object>) result.getOutputData().get("appendedEvent");
        assertEquals(2, appended.get("sequence"));
    }

    @Test
    void appendedEventUsesFixedTimestamp() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "existingEvents", new ArrayList<>(),
                "newEvent", Map.of("type", "FundsDeposited", "data", Map.of("amount", 100)),
                "nextSequence", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> appended =
                (Map<String, Object>) result.getOutputData().get("appendedEvent");
        assertEquals("2026-03-08T10:05:00Z", appended.get("timestamp"));
    }

    @Test
    void returnsTotalEventCount() {
        List<Map<String, Object>> existing = new ArrayList<>();
        existing.add(Map.of("sequence", 1, "type", "AccountCreated"));
        existing.add(Map.of("sequence", 2, "type", "FundsDeposited"));
        existing.add(Map.of("sequence", 3, "type", "FundsWithdrawn"));
        existing.add(Map.of("sequence", 4, "type", "FundsDeposited"));

        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "existingEvents", existing,
                "newEvent", Map.of("type", "FundsDeposited", "data", Map.of("amount", 250, "source", "check_deposit")),
                "nextSequence", 5));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("totalEvents"));
    }

    @Test
    void appendedEventPreservesType() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "existingEvents", new ArrayList<>(),
                "newEvent", Map.of("type", "FundsDeposited", "data", Map.of("amount", 250)),
                "nextSequence", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> appended =
                (Map<String, Object>) result.getOutputData().get("appendedEvent");
        assertEquals("FundsDeposited", appended.get("type"));
    }

    @Test
    void appendedEventPreservesData() {
        Map<String, Object> eventData = Map.of("amount", 250, "source", "check_deposit");
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "existingEvents", new ArrayList<>(),
                "newEvent", Map.of("type", "FundsDeposited", "data", eventData),
                "nextSequence", 1));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> appended =
                (Map<String, Object>) result.getOutputData().get("appendedEvent");
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) appended.get("data");
        assertEquals(250, data.get("amount"));
        assertEquals("check_deposit", data.get("source"));
    }

    @Test
    void handlesNullExistingEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregateId", "acct-1001");
        input.put("existingEvents", null);
        input.put("newEvent", Map.of("type", "FundsDeposited", "data", Map.of("amount", 100)));
        input.put("nextSequence", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allEvents =
                (List<Map<String, Object>>) result.getOutputData().get("allEvents");
        assertEquals(1, allEvents.size());
    }

    @Test
    void handlesNullNewEvent() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregateId", "acct-1001");
        input.put("existingEvents", new ArrayList<>());
        input.put("newEvent", null);
        input.put("nextSequence", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> appended =
                (Map<String, Object>) result.getOutputData().get("appendedEvent");
        assertEquals("Unknown", appended.get("type"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
