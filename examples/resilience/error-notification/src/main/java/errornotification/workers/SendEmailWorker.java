package errornotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for en_send_email -- sends an email notification.
 *
 * Returns sent=true and the recipient address from input (defaults to "oncall@example.com").
 */
public class SendEmailWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String to = task.getInputData().get("to") != null
                ? String.valueOf(task.getInputData().get("to"))
                : "oncall@example.com";

        System.out.println("  [en_send_email] Sending email notification to " + to);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("to", to);

        return result;
    }
}
