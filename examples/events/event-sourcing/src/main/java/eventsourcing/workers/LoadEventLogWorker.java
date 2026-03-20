package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads the event log for a given aggregate. Returns a fixed set of four
 * bank-account domain events plus metadata about the log length.
 */
public class LoadEventLogWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ev_load_event_log";
    }

    @Override
    public TaskResult execute(Task task) {
        String aggregateId = (String) task.getInputData().get("aggregateId");
        String aggregateType = (String) task.getInputData().get("aggregateType");

        if (aggregateId == null || aggregateId.isBlank()) {
            aggregateId = "unknown";
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            aggregateType = "unknown";
        }

        System.out.println("  [ev_load_event_log] Loading event log for " + aggregateType + " " + aggregateId);

        List<Map<String, Object>> events = List.of(
                Map.of(
                        "sequence", 1,
                        "type", "AccountCreated",
                        "timestamp", "2026-01-15T09:00:00Z",
                        "data", Map.of("owner", "Alice", "initialBalance", 0)
                ),
                Map.of(
                        "sequence", 2,
                        "type", "FundsDeposited",
                        "timestamp", "2026-01-15T09:15:00Z",
                        "data", Map.of("amount", 1000, "source", "wire_transfer")
                ),
                Map.of(
                        "sequence", 3,
                        "type", "FundsWithdrawn",
                        "timestamp", "2026-01-15T09:30:00Z",
                        "data", Map.of("amount", 200, "destination", "ATM")
                ),
                Map.of(
                        "sequence", 4,
                        "type", "FundsDeposited",
                        "timestamp", "2026-01-15T09:45:00Z",
                        "data", Map.of("amount", 500, "source", "direct_deposit")
                )
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("eventCount", 4);
        result.getOutputData().put("nextSequence", 5);
        return result;
    }
}
