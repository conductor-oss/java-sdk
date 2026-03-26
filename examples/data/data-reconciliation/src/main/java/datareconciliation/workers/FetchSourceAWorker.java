package datareconciliation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Fetches records from source A (billing system).
 * Input: source
 * Output: records, recordCount
 */
public class FetchSourceAWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rc_fetch_source_a";
    }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = List.of(
                Map.of("orderId", "ORD-001", "amount", 150.00, "status", "completed", "customer", "Alice"),
                Map.of("orderId", "ORD-002", "amount", 89.99, "status", "completed", "customer", "Bob"),
                Map.of("orderId", "ORD-003", "amount", 250.00, "status", "pending", "customer", "Carol"),
                Map.of("orderId", "ORD-004", "amount", 45.50, "status", "completed", "customer", "Dave"),
                Map.of("orderId", "ORD-005", "amount", 320.00, "status", "completed", "customer", "Eve")
        );

        System.out.println("  [source_a] Fetched " + records.size() + " records from billing system");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("records", records);
        result.getOutputData().put("recordCount", records.size());
        return result;
    }
}
