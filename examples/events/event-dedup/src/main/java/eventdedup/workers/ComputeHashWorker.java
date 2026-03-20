package eventdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Computes a deterministic hash of the event payload for deduplication.
 * Uses a fixed hash value for demonstration purposes.
 *
 * Input:  {eventId, payload}
 * Output: {hash: "sha256_fixed_hash_001"}
 */
public class ComputeHashWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dd_compute_hash";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        Object payload = task.getInputData().get("payload");

        System.out.println("  [dd_compute_hash] Computing hash for event: " + eventId);

        // Fixed deterministic hash for demonstration
        String hash = "sha256_fixed_hash_001";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("hash", hash);
        return result;
    }
}
