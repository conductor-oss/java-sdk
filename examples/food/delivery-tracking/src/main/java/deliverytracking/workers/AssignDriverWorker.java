package deliverytracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AssignDriverWorker implements Worker {
    @Override public String getTaskDefName() { return "dlt_assign_driver"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [driver] Assigning driver for order " + task.getInputData().get("orderId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("driverId", "DRV-55");
        result.addOutputData("name", "Mike");
        result.addOutputData("eta", "8 min");
        return result;
    }
}
