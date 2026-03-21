package permissionsync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DiffWorker implements Worker {
    @Override public String getTaskDefName() { return "pms_diff"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [diff] Found 6 permission differences");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("diffs", List.of(
                Map.of("role", "admin", "missing", 1),
                Map.of("role", "editor", "missing", 2),
                Map.of("role", "viewer", "missing", 3)));
        result.getOutputData().put("totalDiffs", 6);
        return result;
    }
}
