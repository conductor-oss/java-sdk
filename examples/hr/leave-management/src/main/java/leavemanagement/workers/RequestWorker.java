package leavemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RequestWorker implements Worker {
    @Override public String getTaskDefName() { return "lvm_request"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [request] " + task.getInputData().get("employeeId") +
                " requests " + task.getInputData().get("days") + " days " +
                task.getInputData().get("leaveType") + " leave");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "LV-601");
        result.getOutputData().put("submitted", true);
        return result;
    }
}
