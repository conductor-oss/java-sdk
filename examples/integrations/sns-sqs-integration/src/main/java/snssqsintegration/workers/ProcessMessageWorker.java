package snssqsintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a received SQS message.
 * Input: messageBody, receiptHandle
 * Output: processed, deletedFromQueue, processedAt
 */
public class ProcessMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sns_process_message";
    }

    @Override
    public TaskResult execute(Task task) {
        Object messageBody = task.getInputData().get("messageBody");
        String receiptHandle = (String) task.getInputData().get("receiptHandle");
        System.out.println("  [process] Processed message: " + messageBody + ", deleting receipt " + receiptHandle);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("deletedFromQueue", true);
        result.getOutputData().put("processedAt", "" + java.time.Instant.now().toString());
        return result;
    }
}
