package cateringmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExecuteWorker implements Worker {
    @Override public String getTaskDefName() { return "cat_execute"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [execute] Executing catering for " + task.getInputData().get("eventDate"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("executed", true);
        result.addOutputData("staff", 8);
        result.addOutputData("satisfaction", "excellent");
        return result;
    }
}
