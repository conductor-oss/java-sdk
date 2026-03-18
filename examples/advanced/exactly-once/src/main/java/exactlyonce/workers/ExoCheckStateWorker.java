package exactlyonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks whether a message has already been processed using an in-memory
 * ConcurrentHashMap. Returns the current processing state and sequence number.
 *
 * Input: messageId, lockToken
 * Output: currentState ("pending" or "completed"), alreadyProcessed (boolean), sequenceNumber
 */
public class ExoCheckStateWorker implements Worker {

    // Shared state store: messageId -> processing state
    static final ConcurrentHashMap<String, String> STATE_STORE = new ConcurrentHashMap<>();
    // Track sequence numbers per messageId
    private static final ConcurrentHashMap<String, Integer> SEQUENCE_STORE = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "exo_check_state";
    }

    @Override
    public TaskResult execute(Task task) {
        String messageId = (String) task.getInputData().get("messageId");
        if (messageId == null) messageId = "unknown";

        String currentState = STATE_STORE.getOrDefault(messageId, "pending");
        boolean alreadyProcessed = "completed".equals(currentState);
        int seqNum = SEQUENCE_STORE.merge(messageId, 1, Integer::sum);

        System.out.println("  [check] messageId=" + messageId + " state=" + currentState
                + " alreadyProcessed=" + alreadyProcessed + " seq=" + seqNum);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("currentState", currentState);
        result.getOutputData().put("alreadyProcessed", alreadyProcessed);
        result.getOutputData().put("sequenceNumber", seqNum);
        return result;
    }

    /** Clear state for testing. */
    static void clearState() {
        STATE_STORE.clear();
        SEQUENCE_STORE.clear();
    }
}
