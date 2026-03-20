package kitchenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepWorker implements Worker {
    @Override public String getTaskDefName() { return "kit_prep"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [prep] Prepping ingredients for order " + task.getInputData().get("orderId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("prepped", true);
        result.addOutputData("prepTime", "8 min");
        return result;
    }
}
