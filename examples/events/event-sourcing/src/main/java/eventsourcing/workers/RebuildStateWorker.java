package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays all events to rebuild the current aggregate state.
 * Uses deterministic logic: for the standard 5-event bank account stream
 * the final balance is 0 + 1000 - 200 + 500 + 250 = 1550.
 */
public class RebuildStateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ev_rebuild_state";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String aggregateId = (String) task.getInputData().get("aggregateId");
        List<Map<String, Object>> allEvents =
                (List<Map<String, Object>>) task.getInputData().get("allEvents");
        String aggregateType = (String) task.getInputData().get("aggregateType");

        if (allEvents == null) {
            allEvents = List.of();
        }

        System.out.println("  [ev_rebuild_state] Rebuilding " + aggregateType
                + " " + aggregateId + " from " + allEvents.size() + " events");

        // Replay events to build state
        String owner = "unknown";
        int balance = 0;
        String status = "active";
        int transactionCount = 0;
        int lastDeposit = 0;

        for (Map<String, Object> event : allEvents) {
            String type = (String) event.get("type");
            Map<String, Object> data = (Map<String, Object>) event.getOrDefault("data", Map.of());

            switch (type) {
                case "AccountCreated":
                    owner = (String) data.getOrDefault("owner", "unknown");
                    balance = data.get("initialBalance") instanceof Number
                            ? ((Number) data.get("initialBalance")).intValue() : 0;
                    status = "active";
                    break;
                case "FundsDeposited":
                    int depositAmount = data.get("amount") instanceof Number
                            ? ((Number) data.get("amount")).intValue() : 0;
                    balance += depositAmount;
                    transactionCount++;
                    lastDeposit = depositAmount;
                    break;
                case "FundsWithdrawn":
                    int withdrawAmount = data.get("amount") instanceof Number
                            ? ((Number) data.get("amount")).intValue() : 0;
                    balance -= withdrawAmount;
                    transactionCount++;
                    break;
                default:
                    break;
            }
        }

        Map<String, Object> currentState = new LinkedHashMap<>();
        currentState.put("owner", owner);
        currentState.put("balance", balance);
        currentState.put("status", status);
        currentState.put("transactionCount", transactionCount);
        currentState.put("lastDeposit", lastDeposit);

        int version = allEvents.size();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("currentState", currentState);
        result.getOutputData().put("version", version);
        return result;
    }
}
