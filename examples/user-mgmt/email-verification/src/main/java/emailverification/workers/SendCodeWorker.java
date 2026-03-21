package emailverification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Sends a verification code to the user's email.
 * Input: email, userId
 * Output: verificationCode, sentAt, expiresIn
 */
public class SendCodeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "emv_send_code";
    }

    @Override
    public TaskResult execute(Task task) {
        String email = (String) task.getInputData().get("email");
        String code = "482917";

        System.out.println("  [send] Verification code sent to " + email);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verificationCode", code);
        result.getOutputData().put("sentAt", Instant.now().toString());
        result.getOutputData().put("expiresIn", 600);
        return result;
    }
}
