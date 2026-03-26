package workflowmigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WmImportNewWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wm_import_new";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [import] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("imported", true);
        result.getOutputData().put("importedTaskCount", 8);
        result.getOutputData().put("newWorkflowId", "WF-MIG-001");
        return result;
    }
}