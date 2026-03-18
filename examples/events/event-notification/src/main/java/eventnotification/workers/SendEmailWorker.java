package eventnotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends an email notification to a recipient.
 * Input: recipientId, subject, body
 * Output: deliveryStatus ("sent"), channel ("email")
 */
public class SendEmailWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_send_email";
    }

    @Override
    public TaskResult execute(Task task) {
        String recipientId = (String) task.getInputData().get("recipientId");
        String subject = (String) task.getInputData().get("subject");

        System.out.println("  [en_send_email] Sent to " + recipientId + ": \"" + subject + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliveryStatus", "sent");
        result.getOutputData().put("channel", "email");
        return result;
    }
}
