package exclusivejoin.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * perform  Vendor C responding to a product query.
 * Returns deterministic price and response time data.
 * Vendor C has the fastest response time.
 */
public class VendorCWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ej_vendor_c";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        if (query == null || query.isBlank()) {
            query = "unknown-product";
        }

        System.out.println("  [ej_vendor_c] Vendor C responding to query: " + query);

        Map<String, Object> vendorResult = new LinkedHashMap<>();
        vendorResult.put("vendor", "C");
        vendorResult.put("price", 55.00);
        vendorResult.put("responseTime", 150);
        vendorResult.put("query", query);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("vendorResult", vendorResult);
        return result;
    }
}
