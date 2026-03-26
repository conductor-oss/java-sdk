package deliverytracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeliverWorker implements Worker {
    @Override public String getTaskDefName() { return "dlt_deliver"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [deliver] Order " + task.getInputData().get("orderId") + " delivered by driver " + task.getInputData().get("driverId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("delivered", true);
        result.addOutputData("deliveryTime", "7:02 PM");
        return result;
    }
}
