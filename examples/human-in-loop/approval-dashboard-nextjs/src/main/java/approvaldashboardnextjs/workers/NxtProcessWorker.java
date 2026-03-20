package approvaldashboardnextjs.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for nxt_process -- processes an approval request before
 * it reaches the WAIT task for human review.
 *
 * Returns { processed: true } to indicate the request has been
 * validated and is ready for the approval dashboard.
 */
public class NxtProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nxt_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String type = (String) task.getInputData().getOrDefault("type", "unknown");
        String title = (String) task.getInputData().getOrDefault("title", "untitled");
        Object amount = task.getInputData().getOrDefault("amount", 0);
        String requester = (String) task.getInputData().getOrDefault("requester", "unknown");

        System.out.println("  [nxt_process] Processing approval request:");
        System.out.println("    Type:      " + type);
        System.out.println("    Title:     " + title);
        System.out.println("    Amount:    " + amount);
        System.out.println("    Requester: " + requester);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("type", type);
        result.getOutputData().put("title", title);
        result.getOutputData().put("amount", amount);
        result.getOutputData().put("requester", requester);

        return result;
    }
}
