package idempotentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Skips processing for a message that was already handled.
 * Returns the reason for skipping and the previous result hash so callers
 * can verify the original processing outcome.
 *
 * Input:  messageId, previousResult
 * Output: skipped, reason, messageId, previousResult
 */
public class SkipWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "idp_skip";
    }

    @Override
    public TaskResult execute(Task task) {
        String msgId = (String) task.getInputData().get("messageId");
        Object previousResult = task.getInputData().get("previousResult");
        if (msgId == null) {
            msgId = "unknown";
        }

        System.out.println("[idp_skip] Skipping duplicate message " + msgId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("skipped", true);
        result.getOutputData().put("reason",
                "Duplicate message — already processed with result: " + previousResult);
        result.getOutputData().put("messageId", msgId);
        result.getOutputData().put("previousResult", previousResult);
        return result;
    }
}
