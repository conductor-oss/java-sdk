package eventsourcing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RebuildStateWorkerTest {

    private final RebuildStateWorker worker = new RebuildStateWorker();

    @Test
    void taskDefName() {
        assertEquals("ev_rebuild_state", worker.getTaskDefName());
    }

    @Test
    void rebuildsCorrectBalanceFromFiveEvents() {
        List<Map<String, Object>> events = buildStandardFiveEvents();
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        assertEquals(1550, state.get("balance"));
    }

    @Test
    void rebuildsCorrectOwner() {
        List<Map<String, Object>> events = buildStandardFiveEvents();
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        assertEquals("Alice", state.get("owner"));
    }

    @Test
    void rebuildsCorrectTransactionCount() {
        List<Map<String, Object>> events = buildStandardFiveEvents();
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        // 3 deposits + 1 withdrawal = 4 transactions (AccountCreated is not a transaction)
        assertEquals(4, state.get("transactionCount"));
    }

    @Test
    void rebuildsCorrectLastDeposit() {
        List<Map<String, Object>> events = buildStandardFiveEvents();
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        assertEquals(250, state.get("lastDeposit"));
    }

    @Test
    void returnsVersionEqualToEventCount() {
        List<Map<String, Object>> events = buildStandardFiveEvents();
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        assertEquals(5, result.getOutputData().get("version"));
    }

    @Test
    void statusIsActive() {
        List<Map<String, Object>> events = buildStandardFiveEvents();
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        assertEquals("active", state.get("status"));
    }

    @Test
    void handlesEmptyEventList() {
        Task task = taskWith(Map.of(
                "aggregateId", "acct-1001",
                "allEvents", new ArrayList<>(),
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        assertEquals(0, state.get("balance"));
        assertEquals(0, result.getOutputData().get("version"));
    }

    @Test
    void handlesNullAllEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("aggregateId", "acct-1001");
        input.put("allEvents", null);
        input.put("aggregateType", "BankAccount");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("version"));
    }

    @Test
    void singleAccountCreatedEventYieldsZeroBalance() {
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(Map.of(
                "sequence", 1,
                "type", "AccountCreated",
                "timestamp", "2026-01-15T09:00:00Z",
                "data", Map.of("owner", "Bob", "initialBalance", 0)));
        Task task = taskWith(Map.of(
                "aggregateId", "acct-2002",
                "allEvents", events,
                "aggregateType", "BankAccount"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) result.getOutputData().get("currentState");
        assertEquals("Bob", state.get("owner"));
        assertEquals(0, state.get("balance"));
        assertEquals(1, result.getOutputData().get("version"));
    }

    private List<Map<String, Object>> buildStandardFiveEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        events.add(Map.of(
                "sequence", 1,
                "type", "AccountCreated",
                "timestamp", "2026-01-15T09:00:00Z",
                "data", Map.of("owner", "Alice", "initialBalance", 0)));
        events.add(Map.of(
                "sequence", 2,
                "type", "FundsDeposited",
                "timestamp", "2026-01-15T09:15:00Z",
                "data", Map.of("amount", 1000, "source", "wire_transfer")));
        events.add(Map.of(
                "sequence", 3,
                "type", "FundsWithdrawn",
                "timestamp", "2026-01-15T09:30:00Z",
                "data", Map.of("amount", 200, "destination", "ATM")));
        events.add(Map.of(
                "sequence", 4,
                "type", "FundsDeposited",
                "timestamp", "2026-01-15T09:45:00Z",
                "data", Map.of("amount", 500, "source", "direct_deposit")));
        events.add(Map.of(
                "sequence", 5,
                "type", "FundsDeposited",
                "timestamp", "2026-03-08T10:05:00Z",
                "data", Map.of("amount", 250, "source", "check_deposit")));
        return events;
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
