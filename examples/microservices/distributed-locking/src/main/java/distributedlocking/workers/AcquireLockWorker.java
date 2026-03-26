package distributedlocking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AcquireLockWorker implements Worker {
    @Override public String getTaskDefName() { return "dl_acquire_lock"; }
    @Override public TaskResult execute(Task task) {
        String resourceId = (String) task.getInputData().getOrDefault("resourceId", "unknown");
        Object ttl = task.getInputData().getOrDefault("ttlSeconds", 30);
        System.out.println("  [lock] Acquired lock on " + resourceId + " (TTL: " + ttl + "s)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("lockToken", "LOCK-" + System.currentTimeMillis());
        r.getOutputData().put("acquired", true);
        return r;
    }
}
