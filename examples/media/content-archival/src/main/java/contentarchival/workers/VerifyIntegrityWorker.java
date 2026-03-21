package contentarchival.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyIntegrityWorker implements Worker {
    @Override public String getTaskDefName() { return "car_verify_integrity"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [verify] Processing " + task.getInputData().getOrDefault("verified", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("verified", true);
        r.getOutputData().put("checksumMatch", true);
        r.getOutputData().put("verifiedAt", "2026-03-08T04:35:00Z");
        r.getOutputData().put("retentionPolicy", "7_years");
        return r;
    }
}
