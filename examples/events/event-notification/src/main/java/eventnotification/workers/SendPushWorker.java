package eventnotification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Sends a push notification to a recipient.
 * Input: recipientId, title, body
 * Output: deliveryStatus ("sent"), channel ("push")
 */
public class SendPushWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "en_send_push";
    }

    @Override
    public TaskResult execute(Task task) {
        String recipientId = (String) task.getInputData().get("recipientId");
        String title = (String) task.getInputData().get("title");

        System.out.println("  [en_send_push] Sent to " + recipientId + ": \"" + title + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deliveryStatus", "sent");
        result.getOutputData().put("channel", "push");
        return result;
    }
}
