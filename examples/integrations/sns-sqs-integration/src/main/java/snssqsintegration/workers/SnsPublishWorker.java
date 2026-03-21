package snssqsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Publishes a message to an SNS topic.
 * Input: topicArn, message
 * Output: messageId, sequenceNumber
 */
public class SnsPublishWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sns_publish";
    }

    @Override
    public TaskResult execute(Task task) {
        String topicArn = (String) task.getInputData().get("topicArn");
        String messageId = "sns-msg-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [publish] Published " + messageId + " to " + topicArn);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("messageId", "" + messageId);
        result.getOutputData().put("sequenceNumber", System.currentTimeMillis());
        return result;
    }
}
