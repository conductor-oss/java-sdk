package foodordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fod_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        System.out.println("  [prepare] Preparing order " + orderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("prepTime", "18 min");
        result.addOutputData("ready", true);
        return result;
    }
}
