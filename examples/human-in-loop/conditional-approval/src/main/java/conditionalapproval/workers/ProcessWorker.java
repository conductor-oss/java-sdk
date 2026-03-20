package conditionalapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for car_process — marks a request as processed after all approvals.
 *
 * Returns { processed: true } on completion.
 */
public class ProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "car_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        String tier = (String) task.getInputData().get("tier");

        System.out.println("  [car_process] " + requestId + " approved (" + tier + " tier)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
