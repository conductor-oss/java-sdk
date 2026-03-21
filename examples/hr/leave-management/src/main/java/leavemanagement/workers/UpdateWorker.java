package leavemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateWorker implements Worker {
    @Override public String getTaskDefName() { return "lvm_update"; }

    @Override
    public TaskResult execute(Task task) {
        int days = Integer.parseInt(String.valueOf(task.getInputData().get("days")));
        int remaining = 15 - days;
        System.out.println("  [update] Balance updated: " + remaining + " days remaining");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("remainingBalance", remaining);
        result.getOutputData().put("updated", true);
        return result;
    }
}
