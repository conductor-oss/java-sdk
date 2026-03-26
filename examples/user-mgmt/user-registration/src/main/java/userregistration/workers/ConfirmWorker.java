package userregistration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.UUID;

/**
 * Sends confirmation email.
 * Input: userId, email
 * Output: confirmationSent, token
 */
public class ConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ur_confirm";
    }

    @Override
    public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        String token = "tok_" + UUID.randomUUID().toString().substring(0, 8);

        System.out.println("  [confirm] Confirmation email sent to " + email);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmationSent", true);
        result.getOutputData().put("token", token);
        return result;
    }
}
