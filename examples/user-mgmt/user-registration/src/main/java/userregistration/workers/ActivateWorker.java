package userregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Activates user account.
 * Input: userId
 * Output: active, activatedAt
 */
public class ActivateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ur_activate";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");

        System.out.println("  [activate] User " + userId + " activated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("active", true);
        result.getOutputData().put("activatedAt", Instant.now().toString());
        return result;
    }
}
