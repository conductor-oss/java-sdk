package distributedlocking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReleaseLockWorker implements Worker {
    @Override public String getTaskDefName() { return "dl_release_lock"; }
    @Override public TaskResult execute(Task task) {
        String lockToken = (String) task.getInputData().getOrDefault("lockToken", "unknown");
        System.out.println("  [unlock] Released lock " + lockToken);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("released", true);
        return r;
    }
}
