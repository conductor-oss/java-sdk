package claimcheck.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Passes the lightweight claim check reference through the pipeline.
 * Input: claimCheckId, storageLocation
 * Output: claimCheckId, storageLocation, referenceSizeBytes
 */
public class PassReferenceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "clc_pass_reference";
    }

    @Override
    public TaskResult execute(Task task) {
        String claimCheckId = (String) task.getInputData().get("claimCheckId");
        if (claimCheckId == null) {
            claimCheckId = "";
        }
        String storageLocation = (String) task.getInputData().get("storageLocation");
        if (storageLocation == null) {
            storageLocation = "";
        }

        System.out.println("  [reference] Passing lightweight reference: " + claimCheckId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("claimCheckId", claimCheckId);
        result.getOutputData().put("storageLocation", storageLocation);
        result.getOutputData().put("referenceSizeBytes", claimCheckId.length());
        return result;
    }
}
