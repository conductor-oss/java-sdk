package taskdedup.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Hashes the input payload to produce a deterministic deduplication key.
 * Input: payload
 * Output: hash
 */
public class HashInputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdd_hash_input";
    }

    @Override
    public TaskResult execute(Task task) {
        Object payload = task.getInputData().get("payload");
        String payloadStr = payload != null ? payload.toString() : "{}";

        // Simple hash process matching the TypeScript version
        int hash = 0;
        for (int i = 0; i < payloadStr.length(); i++) {
            hash = ((hash << 5) - hash + payloadStr.charAt(i));
        }
        String hashStr = "sha256:" + String.format("%016x", Math.abs((long) hash));

        System.out.println("  [hash] Input hashed: " + hashStr);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("hash", hashStr);
        return result;
    }
}
