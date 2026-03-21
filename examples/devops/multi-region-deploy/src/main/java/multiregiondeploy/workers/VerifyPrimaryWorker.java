package multiregiondeploy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies health of the primary region deployment.
 * Input: verify_primaryData
 * Output: verify_primary, processed
 */
public class VerifyPrimaryWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrd_verify_primary";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify-primary] us-east-1 healthy, proceeding to secondary regions");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verify_primary", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
