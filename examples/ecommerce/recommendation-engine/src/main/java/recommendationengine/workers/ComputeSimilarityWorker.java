package recommendationengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Computes similarity scores between user behavior and candidate products.
 */
public class ComputeSimilarityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rec_compute_similarity";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        if (userId == null) userId = "unknown";

        System.out.println("  [similarity] Computing similarity scores for " + userId);

        List<Map<String, Object>> similarProducts = List.of(
                Map.of("sku", "SKU-206", "score", 0.95, "category", "audio"),
                Map.of("sku", "SKU-313", "score", 0.88, "category", "electronics"),
                Map.of("sku", "SKU-451", "score", 0.82, "category", "electronics"),
                Map.of("sku", "SKU-119", "score", 0.76, "category", "accessories"),
                Map.of("sku", "SKU-520", "score", 0.71, "category", "audio")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("similarProducts", similarProducts);
        return result;
    }
}
