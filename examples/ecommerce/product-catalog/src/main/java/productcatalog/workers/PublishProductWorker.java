package productcatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publishes a product to the storefront.
 */
public class PublishProductWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prd_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");

        System.out.println("  [publish] Product " + productId + " published to storefront");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("published", true);
        output.put("publishedAt", Instant.now().toString());
        output.put("url", "/products/" + productId);
        result.setOutputData(output);
        return result;
    }
}
