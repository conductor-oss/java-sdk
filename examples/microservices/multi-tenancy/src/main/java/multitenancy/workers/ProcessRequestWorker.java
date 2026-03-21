package multitenancy.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ProcessRequestWorker implements Worker {
    @Override public String getTaskDefName() { return "mt_process_request"; }
    @Override public TaskResult execute(Task task) {
        String tenantId = (String) task.getInputData().getOrDefault("tenantId", "unknown");
        String tier = (String) task.getInputData().getOrDefault("tier", "unknown");
        String action = (String) task.getInputData().getOrDefault("action", "unknown");
        System.out.println("  [process] Tenant " + tenantId + " (" + tier + "): " + action);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "processed");
        r.getOutputData().put("cost", 0.05);
        return r;
    }
}
