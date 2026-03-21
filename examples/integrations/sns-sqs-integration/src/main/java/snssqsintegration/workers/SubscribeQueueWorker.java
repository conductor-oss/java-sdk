package snssqsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Subscribes an SQS queue to an SNS topic.
 * Input: topicArn, queueUrl
 * Output: subscriptionArn, protocol
 */
public class SubscribeQueueWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sns_subscribe_queue";
    }

    @Override
    public TaskResult execute(Task task) {
        String topicArn = (String) task.getInputData().get("topicArn");
        String queueUrl = (String) task.getInputData().get("queueUrl");
        String subscriptionArn = topicArn + ":sub-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [subscribe] Queue " + queueUrl + " subscribed to " + topicArn);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("subscriptionArn", "" + subscriptionArn);
        result.getOutputData().put("protocol", "sqs");
        return result;
    }
}
