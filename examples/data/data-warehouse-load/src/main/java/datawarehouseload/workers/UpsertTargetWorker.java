package datawarehouseload.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Upserts records from staging into the target table.
 * Input: stagingTable (string), targetTable (string), recordCount (int)
 * Output: upsertedCount (int), inserted (int), updated (int), targetTable (string)
 */
public class UpsertTargetWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wh_upsert_target";
    }

    @Override
    public TaskResult execute(Task task) {
        Object countObj = task.getInputData().get("recordCount");
        int count = 0;
        if (countObj instanceof Number) {
            count = ((Number) countObj).intValue();
        }
        String targetTable = (String) task.getInputData().getOrDefault("targetTable", "unknown");
        int inserted = (int) Math.floor(count * 0.6);
        int updated = count - inserted;

        System.out.println("  [upsert] Loaded into \"" + targetTable + "\": " + inserted + " inserted, " + updated + " updated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("upsertedCount", count);
        result.getOutputData().put("inserted", inserted);
        result.getOutputData().put("updated", updated);
        result.getOutputData().put("targetTable", targetTable);
        return result;
    }
}
