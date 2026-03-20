package leavemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NotifyWorker implements Worker {
    @Override public String getTaskDefName() { return "lvm_notify"; }

    @Override
    public TaskResult execute(Task task) {
        boolean approved = Boolean.parseBoolean(String.valueOf(task.getInputData().get("approved")));
        System.out.println("  [notify] " + task.getInputData().get("employeeId") +
                " notified: leave " + (approved ? "approved" : "denied"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("notified", true);
        return result;
    }
}
