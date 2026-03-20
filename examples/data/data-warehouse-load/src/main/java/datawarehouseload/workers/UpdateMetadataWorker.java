package datawarehouseload.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Updates warehouse metadata after a successful load.
 * Input: targetTable (string), recordsLoaded (int), validationPassed (boolean)
 * Output: summary (string), lastLoadTime (string)
 */
public class UpdateMetadataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wh_update_metadata";
    }

    @Override
    public TaskResult execute(Task task) {
        String targetTable = (String) task.getInputData().getOrDefault("targetTable", "unknown");
        Object recordsLoadedObj = task.getInputData().get("recordsLoaded");
        int recordsLoaded = 0;
        if (recordsLoadedObj instanceof Number) {
            recordsLoaded = ((Number) recordsLoadedObj).intValue();
        }
        Object validationObj = task.getInputData().get("validationPassed");
        boolean validationPassed = Boolean.TRUE.equals(validationObj);

        String summary = "Warehouse load complete: " + recordsLoaded + " records into \"" + targetTable + "\", validation=" + validationPassed;
        System.out.println("  [metadata] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("lastLoadTime", Instant.now().toString());
        return result;
    }
}
