package exclusivejoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * perform  Vendor B responding to a product query.
 * Returns deterministic price and response time data.
 * Vendor B has the lowest price but slowest response.
 */
public class VendorBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ej_vendor_b";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "unknown-product";
        }

        System.out.println("  [ej_vendor_b] Vendor B responding to query: " + query);

        Map<String, Object> vendorResult = new LinkedHashMap<>();
        vendorResult.put("vendor", "B");
        vendorResult.put("price", 42.50);
        vendorResult.put("responseTime", 450);
        vendorResult.put("query", query);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("vendorResult", vendorResult);
        return result;
    }
}
