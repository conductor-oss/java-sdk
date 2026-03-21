package workflowmigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class WmExportOldWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wm_export_old";
    }

    @Override
    public TaskResult execute(Task task) {
        String workflowName = (String) task.getInputData().getOrDefault("workflowName", "unknown");
        System.out.println("  [export] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("exported", true);
        result.getOutputData().put("definition", java.util.Map.of("name", workflowName, "tasks", 8));
        result.getOutputData().put("format", "legacy_json");
        result.getOutputData().put("taskCount", 8);
        return result;
    }
}