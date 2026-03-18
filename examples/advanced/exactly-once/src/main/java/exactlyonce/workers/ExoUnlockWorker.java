package exactlyonce.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Releases the lock held on a resource key. Verifies that the lock token
 * matches the one currently held before releasing.
 *
 * Input: resourceKey, lockToken
 * Output: unlocked (boolean), previousHolder (the token that was removed)
 */
public class ExoUnlockWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "exo_unlock";
    }

    @Override
    public TaskResult execute(Task task) {
        String resourceKey = (String) task.getInputData().get("resourceKey");
        if (resourceKey == null) resourceKey = "default";

        String lockToken = (String) task.getInputData().get("lockToken");

        // Remove the lock only if it matches the provided token
        boolean unlocked;
        String previousHolder = ExoLockWorker.LOCKS.get(resourceKey);
        if (lockToken != null && lockToken.equals(previousHolder)) {
            ExoLockWorker.LOCKS.remove(resourceKey, lockToken);
            unlocked = true;
        } else if (previousHolder == null) {
            unlocked = true; // Already unlocked
        } else {
            unlocked = false; // Token mismatch
        }

        System.out.println("  [unlock] resourceKey=" + resourceKey + " unlocked=" + unlocked);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("unlocked", unlocked);
        result.getOutputData().put("previousHolder", previousHolder);
        return result;
    }
}
