package distributedlocking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecuteCriticalWorker implements Worker {
    @Override public String getTaskDefName() { return "dl_execute_critical"; }
    @Override public TaskResult execute(Task task) {
        String op = (String) task.getInputData().getOrDefault("operation", "unknown");
        String resourceId = (String) task.getInputData().getOrDefault("resourceId", "unknown");
        System.out.println("  [critical] Executing " + op + " on " + resourceId);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "updated");
        r.getOutputData().put("version", 5);
        return r;
    }
}
