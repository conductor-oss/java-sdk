package eventaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Publishes the aggregated summary batch to the configured destination.
 * Returns a fixed batch ID and publish timestamp.
 */
public class PublishBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eg_publish_batch";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> summary =
                (Map<String, Object>) task.getInputData().get("summary");
        if (summary == null) {
            summary = Map.of();
        }

        String destination = (String) task.getInputData().get("destination");
        if (destination == null || destination.isBlank()) {
            destination = "default_pipeline";
        }

        System.out.println("  [eg_publish_batch] Publishing batch to: " + destination);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("batchId", "batch-fixed-001");
        result.getOutputData().put("published", true);
        result.getOutputData().put("publishedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
