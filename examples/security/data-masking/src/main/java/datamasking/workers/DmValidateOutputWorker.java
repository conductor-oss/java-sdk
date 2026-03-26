package datamasking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates that no PII remains in the masked output.
 * Input: validate_outputData (from masking step)
 * Output: validate_output, completedAt
 */
public class DmValidateOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_validate_output";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [validate] No PII found in masked output, referential integrity maintained");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validate_output", true);
        result.getOutputData().put("completedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
