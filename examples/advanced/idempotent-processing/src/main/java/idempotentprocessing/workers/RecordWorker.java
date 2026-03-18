package idempotentprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Records a successfully processed message in the {@link DedupStore} so that
 * future runs with the same messageId are detected as duplicates.
 *
 * Input:  messageId, resultHash
 * Output: recorded
 */
public class RecordWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "idp_record";
    }

    @Override
    public TaskResult execute(Task task) {
        String msgId = (String) task.getInputData().get("messageId");
        String resultHash = (String) task.getInputData().get("resultHash");
        if (msgId == null) {
            msgId = "unknown";
        }
        if (resultHash == null) {
            resultHash = "none";
        }

        DedupStore.record(msgId, resultHash);
        System.out.println("[idp_record] Recording " + msgId + " as processed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        return result;
    }
}
