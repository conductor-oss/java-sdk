package deliverytracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PickupWorker implements Worker {
    @Override public String getTaskDefName() { return "dlt_pickup"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [pickup] Driver " + task.getInputData().get("driverId") + " picked up order " + task.getInputData().get("orderId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("pickedUp", true);
        result.addOutputData("pickupTime", "6:45 PM");
        return result;
    }
}
