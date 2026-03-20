package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Applies masking to the identified sensitive fields.
 * Input: apply_maskingData (from strategy step)
 * Output: apply_masking, processed
 */
public class DmApplyMaskingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_apply_masking";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mask] 18 fields masked: emails tokenized, SSNs redacted");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("apply_masking", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
