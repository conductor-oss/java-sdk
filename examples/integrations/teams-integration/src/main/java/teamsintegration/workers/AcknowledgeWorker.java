package teamsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Acknowledges a posted Teams message.
 * Input: messageId, channelId
 * Output: acknowledged
 */
public class AcknowledgeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tms_acknowledge";
    }

    @Override
    public TaskResult execute(Task task) {
        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null) {
            messageId = "unknown";
        }

        System.out.println("  [ack] Acknowledged message " + messageId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("acknowledged", true);
        result.getOutputData().put("status", "acknowledged");
        return result;
    }
}
