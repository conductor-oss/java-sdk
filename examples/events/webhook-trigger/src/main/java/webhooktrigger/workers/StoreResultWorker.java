package webhooktrigger.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Stores the transformed data into the target destination and returns
 * a confirmation record with the storage details.
 */
public class StoreResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wt_store_result";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> transformedData = (Map<String, Object>) task.getInputData().get("transformedData");
        if (transformedData == null) {
            transformedData = Map.of();
        }

        String destination = (String) task.getInputData().get("destination");
        if (destination == null || destination.isBlank()) {
            destination = "default_db";
        }

        System.out.println("  [wt_store_result] Storing record to " + destination);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recordId", "rec-fixed-001");
        result.getOutputData().put("storedAt", "2026-01-15T10:00:00Z");
        result.getOutputData().put("destination", destination);
        return result;
    }
}
