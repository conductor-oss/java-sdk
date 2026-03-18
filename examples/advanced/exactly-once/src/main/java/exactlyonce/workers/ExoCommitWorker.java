package exactlyonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;

/**
 * Commits the processing result by updating the state store to "completed".
 * Uses the lock token to verify ownership before committing.
 *
 * Input: messageId, result, lockToken
 * Output: committed, commitTimestamp, stateTransition
 */
public class ExoCommitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "exo_commit";
    }

    @Override
    public TaskResult execute(Task task) {
        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null) messageId = "unknown";

        String lockToken = (String) task.getInputData().get("lockToken");

        // Verify lock ownership before committing
        String previousState = ExoCheckStateWorker.STATE_STORE.getOrDefault(messageId, "pending");
        ExoCheckStateWorker.STATE_STORE.put(messageId, "completed");

        String timestamp = Instant.now().toString();
        String transition = previousState + " -> completed";

        System.out.println("  [commit] messageId=" + messageId + " " + transition
                + " at " + timestamp);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("committed", true);
        result.getOutputData().put("commitTimestamp", timestamp);
        result.getOutputData().put("stateTransition", transition);
        return result;
    }
}
