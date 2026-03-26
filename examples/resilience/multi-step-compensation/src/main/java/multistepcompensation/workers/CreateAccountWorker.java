package multistepcompensation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for msc_create_account — creates an account and returns a deterministic accountId.
 */
public class CreateAccountWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msc_create_account";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [msc_create_account] Creating account...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("accountId", "ACCT-001");

        System.out.println("  [msc_create_account] Account created: ACCT-001");
        return result;
    }
}
