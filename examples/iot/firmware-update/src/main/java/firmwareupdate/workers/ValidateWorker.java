package firmwareupdate.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "fw_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Processing " + task.getInputData().getOrDefault("validated", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("validated", true);
        r.getOutputData().put("checksumMatch", true);
        r.getOutputData().put("signatureValid", true);
        r.getOutputData().put("compatibilityCheck", "passed");
        return r;
    }
}
