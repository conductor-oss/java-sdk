package compliancemonitoring.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class ScanResourcesWorker implements Worker {
    @Override public String getTaskDefName() { return "cpm_scan_resources"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [scan] Scanning resources for " + task.getInputData().get("framework") + " compliance");
        r.getOutputData().put("resourcesScanned", 256);
        r.getOutputData().put("findings", List.of(Map.of("resource","s3-bucket-logs","finding","no encryption")));
        return r;
    }
}
