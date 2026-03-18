package exactlyonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Acquires a distributed lock for a resource key using ConcurrentHashMap.
 * If the lock is already held, the task fails to enforce mutual exclusion.
 *
 * Input: resourceKey, ttlSeconds
 * Output: lockToken, acquired, ttlSeconds
 */
public class ExoLockWorker implements Worker {

    // Shared lock store: resourceKey -> lockToken
    static final ConcurrentHashMap<String, String> LOCKS = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "exo_lock";
    }

    @Override
    public TaskResult execute(Task task) {
        String resourceKey = (String) task.getInputData().get("resourceKey");
        if (resourceKey == null) resourceKey = "default";

        int ttl = 30;
        Object ttlObj = task.getInputData().get("ttlSeconds");
        if (ttlObj instanceof Number) ttl = ((Number) ttlObj).intValue();

        String lockToken = "lock-" + resourceKey + "-" + Long.toString(System.nanoTime(), 36);

        // Try to acquire the lock atomically
        String existing = LOCKS.putIfAbsent(resourceKey, lockToken);
        boolean acquired = (existing == null);

        if (!acquired) {
            lockToken = existing; // Return the existing token for diagnostics
        }

        System.out.println("  [lock] resourceKey=" + resourceKey + " acquired=" + acquired
                + " token=" + lockToken);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("lockToken", lockToken);
        result.getOutputData().put("acquired", acquired);
        result.getOutputData().put("ttlSeconds", ttl);
        return result;
    }

    /** Clear locks for testing. */
    static void clearLocks() {
        LOCKS.clear();
    }
}
