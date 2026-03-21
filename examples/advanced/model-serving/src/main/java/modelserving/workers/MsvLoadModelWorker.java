package modelserving.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MsvLoadModelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msv_load_model";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [load] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("loaded", true);
        result.getOutputData().put("signature", "serving_default");
        result.getOutputData().put("inputShape", java.util.List.of(1, 224, 224, 3));
        result.getOutputData().put("sizeBytes", 95000000);
        return result;
    }
}