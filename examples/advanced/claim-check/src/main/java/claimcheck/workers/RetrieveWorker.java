package claimcheck.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Retrieves the full payload from storage using the claim check reference.
 * Input: claimCheckId, storageLocation
 * Output: payload, retrieved
 */
public class RetrieveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "clc_retrieve";
    }

    @Override
    public TaskResult execute(Task task) {
        String storageLocation = (String) task.getInputData().get("storageLocation");
        if (storageLocation == null) {
            storageLocation = "unknown";
        }

        System.out.println("  [retrieve] Retrieving payload from " + storageLocation);

        Map<String, Object> payload = Map.of(
                "reportId", "RPT-2024-001",
                "data", List.of(
                        Map.of("metric", "cpu_usage", "values", List.of(45, 52, 68, 71, 55)),
                        Map.of("metric", "memory_usage", "values", List.of(62, 64, 70, 68, 65)),
                        Map.of("metric", "disk_io", "values", List.of(120, 130, 145, 110, 125))
                ),
                "generatedAt", "2024-01-15T10:30:00Z"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("payload", payload);
        result.getOutputData().put("retrieved", true);
        return result;
    }
}
