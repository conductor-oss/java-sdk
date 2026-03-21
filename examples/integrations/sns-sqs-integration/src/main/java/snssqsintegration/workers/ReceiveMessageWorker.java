package snssqsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Receives a message from an SQS queue.
 * Input: queueUrl, messageId
 * Output: body, receiptHandle, approximateReceiveCount
 */
public class ReceiveMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sns_receive_message";
    }

    @Override
    public TaskResult execute(Task task) {
        String queueUrl = (String) task.getInputData().get("queueUrl");
        String messageId = (String) task.getInputData().get("messageId");
        String receiptHandle = "rh-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [receive] Message " + messageId + " received from " + queueUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("body", "" + messageId);
        result.getOutputData().put("receiptHandle", "" + receiptHandle);
        result.getOutputData().put("approximateReceiveCount", 1);
        return result;
    }
}
