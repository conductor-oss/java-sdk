package idempotentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Checks if a message has been previously processed by looking it up in the
 * in-memory {@link DedupStore}.
 *
 * Input:  messageId
 * Output: processingState ("unprocessed" or "processed"), previousResult (hash or null)
 */
public class CheckProcessedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "idp_check_processed";
    }

    @Override
    public TaskResult execute(Task task) {
        String msgId = (String) task.getInputData().get("messageId");
        if (msgId == null) {
            msgId = "unknown";
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if (DedupStore.isProcessed(msgId)) {
            String previousHash = DedupStore.getResultHash(msgId);
            System.out.println("[idp_check_processed] Message " + msgId
                    + ": ALREADY processed — skipping");
            result.getOutputData().put("processingState", "processed");
            result.getOutputData().put("previousResult", previousHash);
        } else {
            System.out.println("[idp_check_processed] Message " + msgId
                    + ": NOT previously processed");
            result.getOutputData().put("processingState", "unprocessed");
            result.getOutputData().put("previousResult", null);
        }
        return result;
    }
}
