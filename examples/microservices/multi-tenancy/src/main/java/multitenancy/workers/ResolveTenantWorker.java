package multitenancy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ResolveTenantWorker implements Worker {
    @Override public String getTaskDefName() { return "mt_resolve_tenant"; }
    @Override public TaskResult execute(Task task) {
        String tenantId = (String) task.getInputData().getOrDefault("tenantId", "unknown");
        System.out.println("  [tenant] Resolved " + tenantId + ": enterprise tier");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("tier", "enterprise");
        r.getOutputData().put("region", "us-east-1");
        r.getOutputData().put("isolated", true);
        return r;
    }
}
