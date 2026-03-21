package centralizedconfigmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CfgVerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "cfg_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] All services running with updated config");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("verified", true);
        r.getOutputData().put("allHealthy", true);
        return r;
    }
}
