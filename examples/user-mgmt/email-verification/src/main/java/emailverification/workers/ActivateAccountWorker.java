package emailverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Activates the user account after email verification.
 * Input: userId, verified
 * Output: activated, activatedAt
 */
public class ActivateAccountWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emv_activate";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");

        System.out.println("  [activate] Account " + userId + " activated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("activated", true);
        result.getOutputData().put("activatedAt", Instant.now().toString());
        return result;
    }
}
