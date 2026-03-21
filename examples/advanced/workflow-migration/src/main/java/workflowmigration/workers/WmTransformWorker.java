package workflowmigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WmTransformWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wm_transform";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [transform] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("transformed", java.util.Map.of("name", "migrated_workflow", "tasks", 8));
        result.getOutputData().put("transformSuccess", true);
        return result;
    }
}