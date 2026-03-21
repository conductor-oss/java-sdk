package firmwareupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeployWorker implements Worker {
    @Override public String getTaskDefName() { return "fw_deploy"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [deploy] Processing " + task.getInputData().getOrDefault("deploymentId", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("deploymentId", "FW-DEP-533-001");
        r.getOutputData().put("deployedAt", "2026-03-08T02:00:00Z");
        r.getOutputData().put("rebootRequired", true);
        r.getOutputData().put("rebootScheduled", "2026-03-08T02:01:00Z");
        return r;
    }
}
