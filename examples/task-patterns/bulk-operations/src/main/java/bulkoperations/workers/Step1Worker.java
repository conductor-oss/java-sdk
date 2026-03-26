package bulkoperations.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * First step in the bulk operations workflow.
 * Takes a batchId and returns intermediate data for the batch.
 */
public class Step1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bulk_step1";
    }

    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");
        if (batchId == null || batchId.isBlank()) {
            batchId = "unknown";
        }

        System.out.println("  [bulk_step1] Processing batch: " + batchId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("data", "batch-" + batchId);
        result.getOutputData().put("batchId", batchId);
        return result;
    }
}
