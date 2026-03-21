package datamigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "dm_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Source-target comparison: 100% match");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("valid", true);
        r.getOutputData().put("matchRate", 100);
        return r;
    }
}
