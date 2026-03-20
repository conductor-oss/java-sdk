package datareconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Fetches records from source B (fulfillment system).
 * Input: source
 * Output: records, recordCount
 */
public class FetchSourceBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rc_fetch_source_b";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = List.of(
                Map.of("orderId", "ORD-001", "amount", 150.00, "status", "completed", "customer", "Alice"),
                Map.of("orderId", "ORD-002", "amount", 92.99, "status", "completed", "customer", "Bob"),
                Map.of("orderId", "ORD-003", "amount", 250.00, "status", "completed", "customer", "Carol"),
                Map.of("orderId", "ORD-005", "amount", 320.00, "status", "completed", "customer", "Eve"),
                Map.of("orderId", "ORD-006", "amount", 175.00, "status", "completed", "customer", "Frank")
        );

        System.out.println("  [source_b] Fetched " + records.size() + " records from fulfillment system");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("recordCount", records.size());
        return result;
    }
}
