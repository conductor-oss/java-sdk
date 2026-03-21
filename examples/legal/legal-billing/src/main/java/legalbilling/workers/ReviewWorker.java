package legalbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "lgb_review"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [review] Reviewing time entries for billing compliance");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approvedEntries", task.getInputData().get("timeEntries"));
        result.getOutputData().put("approvedHours", 6.5);
        result.getOutputData().put("adjustments", 0);
        return result;
    }
}
