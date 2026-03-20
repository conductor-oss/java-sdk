package productcatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates product SKU and price.
 */
public class ValidateProductWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prd_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        String sku = (String) task.getInputData().get("sku");
        Object priceObj = task.getInputData().get("price");

        double price = 0;
        if (priceObj instanceof Number) {
            price = ((Number) priceObj).doubleValue();
        } else if (priceObj != null) {
            try { price = Double.parseDouble(priceObj.toString()); } catch (Exception ignored) {}
        }

        boolean valid = sku != null && !sku.isBlank() && price > 0;

        System.out.println("  [validate] Product " + productId + ": SKU=" + sku + ", Price=" + price + " -> valid=" + valid);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("valid", valid);
        output.put("validatedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
