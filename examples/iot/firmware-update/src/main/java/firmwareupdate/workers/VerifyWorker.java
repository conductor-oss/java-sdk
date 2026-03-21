package firmwareupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyWorker implements Worker {
    @Override public String getTaskDefName() { return "fw_verify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Processing " + task.getInputData().getOrDefault("verified", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("verified", true);
        r.getOutputData().put("runningVersion", task.getInputData().get("targetVersion"));
        r.getOutputData().put("bootTime", "2026-03-08T02:01:30Z");
        r.getOutputData().put("selfTestPassed", true);
        r.getOutputData().put("rollbackAvailable", true);
        return r;
    }
}
