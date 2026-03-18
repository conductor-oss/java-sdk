package eventnotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends an SMS notification to a recipient.
 * Input: recipientId, message
 * Output: deliveryStatus ("sent"), channel ("sms")
 */
public class SendSmsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_send_sms";
    }

    @Override
    public TaskResult execute(Task task) {
        String recipientId = (String) task.getInputData().get("recipientId");
        String message = (String) task.getInputData().get("message");

        System.out.println("  [en_send_sms] Sent to " + recipientId + ": \"" + message + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliveryStatus", "sent");
        result.getOutputData().put("channel", "sms");
        return result;
    }
}
