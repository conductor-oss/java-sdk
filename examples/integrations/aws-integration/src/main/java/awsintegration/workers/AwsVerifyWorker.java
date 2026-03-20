package awsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies that all AWS services completed successfully.
 * Input: s3Result, dynamoResult, snsResult
 * Output: verified (boolean)
 */
public class AwsVerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aws_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        String s3Result = (String) task.getInputData().get("s3Result");
        String dynamoResult = (String) task.getInputData().get("dynamoResult");
        String snsResult = (String) task.getInputData().get("snsResult");

        boolean allPresent = s3Result != null && !s3Result.isEmpty()
                && dynamoResult != null && !dynamoResult.isEmpty()
                && snsResult != null && !snsResult.isEmpty();

        System.out.println("  [verify] S3: " + s3Result + ", DynamoDB: " + dynamoResult + ", SNS: " + snsResult);
        System.out.println("  [verify] All services confirmed: " + allPresent);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", allPresent);
        return result;
    }
}
