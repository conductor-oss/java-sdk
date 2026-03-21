package productcatalog.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Indexes a product for search.
 */
public class IndexProductWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "prd_index";
    }

    @Override
    public TaskResult execute(Task task) {
        String productId = (String) task.getInputData().get("productId");
        String name = (String) task.getInputData().get("name");
        String category = (String) task.getInputData().get("category");

        System.out.println("  [index] Product " + productId + " indexed for search (" + name + ", " + category + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("indexed", true);
        output.put("indexedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
