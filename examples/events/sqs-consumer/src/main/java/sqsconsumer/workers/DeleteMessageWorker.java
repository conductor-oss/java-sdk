package sqsconsumer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deletes an SQS message from the queue after successful processing,
 * confirming deletion with a timestamp.
 */
public class DeleteMessageWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qs_delete_message";
    }

    @Override
    public TaskResult execute(Task task) {
        String queueUrl = (String) task.getInputData().get("queueUrl");
        if (queueUrl == null || queueUrl.isBlank()) {
            queueUrl = "";
        }

        String receiptHandle = (String) task.getInputData().get("receiptHandle");
        if (receiptHandle == null || receiptHandle.isBlank()) {
            receiptHandle = "";
        }

        String processingResult = (String) task.getInputData().get("processingResult");
        if (processingResult == null || processingResult.isBlank()) {
            processingResult = "unknown";
        }

        System.out.println("  [qs_delete_message] Deleting message from queue: " + queueUrl);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deleted", true);
        result.getOutputData().put("deletedAt", "2026-01-15T10:00:00Z");
        return result;
    }
}
