package rightsmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class CheckLicenseWorker implements Worker {
    @Override public String getTaskDefName() { return "rts_check_license"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [license] Processing " + task.getInputData().getOrDefault("valid", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true);
        r.getOutputData().put("expirationDate", "2027-12-31");
        r.getOutputData().put("allowedUsages", List.of("streaming"));
        r.getOutputData().put("royaltyRate", 0.12);
        r.getOutputData().put("licenseType", "commercial");
        return r;
    }
}
