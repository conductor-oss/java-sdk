package emailagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends the approved email. Returns a fixed message ID,
 * timestamp, and delivery status.
 */
public class SendEmailWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ea_send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String recipient = (String) task.getInputData().get("recipient");
        if (recipient == null || recipient.isBlank()) {
            recipient = "unknown@example.com";
        }

        String subject = (String) task.getInputData().get("subject");
        if (subject == null || subject.isBlank()) {
            subject = "(no subject)";
        }

        String body = (String) task.getInputData().get("body");
        if (body == null || body.isBlank()) {
            body = "";
        }

        String approved = (String) task.getInputData().get("approved");
        if (approved == null) {
            approved = "false";
        }

        System.out.println("  [ea_send_email] Sending email to: " + recipient);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("messageId", "msg-fixed-abc123");
        result.getOutputData().put("timestamp", "2026-03-08T10:00:00Z");
        result.getOutputData().put("deliveryStatus", "delivered");
        return result;
    }
}
