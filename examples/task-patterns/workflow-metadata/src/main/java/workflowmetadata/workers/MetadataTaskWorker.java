package workflowmetadata.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Simple metadata task worker.
 * Receives category and priority inputs, returns processed: true.
 */
public class MetadataTaskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "md_task";
    }

    @Override
    public TaskResult execute(Task task) {
        String category = (String) task.getInputData().get("category");
        String priority = (String) task.getInputData().get("priority");

        System.out.println("  [md_task] Processing category=" + category + ", priority=" + priority);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
