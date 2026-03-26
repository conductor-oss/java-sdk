package distributedtransactions.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CommitAllWorker implements Worker {
    @Override public String getTaskDefName() { return "dtx_commit_all"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [commit] All 3 transactions committed");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("committed", true);
        r.getOutputData().put("globalTxId", "GTX-" + System.currentTimeMillis());
        return r;
    }
}
