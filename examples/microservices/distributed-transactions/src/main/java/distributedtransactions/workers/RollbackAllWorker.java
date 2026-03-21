package distributedtransactions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RollbackAllWorker implements Worker {
    @Override public String getTaskDefName() { return "dtx_rollback_all"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rollback] All transactions rolled back");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rolledBack", true);
        return r;
    }
}
