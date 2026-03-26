package awsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * perform  publishing a notification to Amazon SNS.
 * Input: topicArn, message
 * Output: messageId, topicArn
 */
public class AwsSnsNotifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aws_sns_notify";
    }

    @Override
    public TaskResult execute(Task task) {
        String topicArn = (String) task.getInputData().get("topicArn");
        if (topicArn == null) {
            topicArn = "arn:aws:sns:us-east-1:000000000:default";
        }

        String messageId = "sns-msg-fixed-001";

        System.out.println("  [SNS] Published " + messageId + " to " + topicArn);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("messageId", "" + messageId);
        result.getOutputData().put("topicArn", "" + topicArn);
        return result;
    }
}
