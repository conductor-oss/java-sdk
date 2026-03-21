package productcatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Adds a new product to the catalog and assigns a product ID.
 */
public class AddProductWorker implements Worker {

    private static final AtomicLong COUNTER = new AtomicLong();

    @Override
    public String getTaskDefName() {
        return "prd_add_product";
    }

    @Override
    public TaskResult execute(Task task) {
        String sku = (String) task.getInputData().get("sku");
        String name = (String) task.getInputData().get("name");

        String productId = "prod-" + Long.toString(System.currentTimeMillis(), 36) + "-" + COUNTER.incrementAndGet();

        System.out.println("  [add] Product " + productId + ": " + name + " (" + sku + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("productId", productId);
        output.put("createdAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
