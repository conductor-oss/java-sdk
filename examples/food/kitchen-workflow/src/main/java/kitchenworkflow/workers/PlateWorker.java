package kitchenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PlateWorker implements Worker {
    @Override public String getTaskDefName() { return "kit_plate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [plate] Plating order " + task.getInputData().get("orderId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("plated", true);
        result.addOutputData("presentation", "garnished");
        return result;
    }
}
