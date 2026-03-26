package permissionsync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ScanSystemsWorker implements Worker {
    @Override public String getTaskDefName() { return "pms_scan_systems"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        String source = (String) task.getInputData().get("source");
        List<String> targets = (List<String>) task.getInputData().get("targets");
        System.out.println("  [scan] Scanned permissions from " + source + " and " + (targets != null ? targets.size() : 0) + " targets");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sourcePermissions", Map.of("admin", 5, "editor", 12, "viewer", 45));
        result.getOutputData().put("targetPermissions", Map.of("admin", 4, "editor", 10, "viewer", 42));
        result.getOutputData().put("scannedAt", Instant.now().toString());
        return result;
    }
}
