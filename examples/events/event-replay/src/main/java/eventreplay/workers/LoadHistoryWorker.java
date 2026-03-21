package eventreplay.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads event history from the specified source stream within the given time range.
 * Returns a fixed set of 6 events representing various order and payment events.
 */
public class LoadHistoryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ep_load_history";
    }

    @Override
    public TaskResult execute(Task task) {
        String sourceStream = (String) task.getInputData().get("sourceStream");
        if (sourceStream == null || sourceStream.isBlank()) {
            sourceStream = "default-stream";
        }

        String startTime = (String) task.getInputData().get("startTime");
        if (startTime == null || startTime.isBlank()) {
            startTime = "";
        }

        String endTime = (String) task.getInputData().get("endTime");
        if (endTime == null || endTime.isBlank()) {
            endTime = "";
        }

        System.out.println("  [ep_load_history] Loading events from " + sourceStream
                + " between " + startTime + " and " + endTime);

        List<Map<String, Object>> events = List.of(
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

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("totalLoaded", 6);
        return result;
    }
}
