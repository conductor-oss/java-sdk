package kitchenworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CookWorker implements Worker {
    @Override public String getTaskDefName() { return "kit_cook"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cook] Cooking order " + task.getInputData().get("orderId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("cooked", true);
        result.addOutputData("cookTime", "14 min");
        result.addOutputData("temp", "165F");
        return result;
    }
}
