package batchmltraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BmlEvaluateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bml_evaluate";
    }

    @Override
    public TaskResult execute(Task task) {
        double acc1 = task.getInputData().get("model1Accuracy") instanceof Number ? ((Number) task.getInputData().get("model1Accuracy")).doubleValue() : 0;
        double acc2 = task.getInputData().get("model2Accuracy") instanceof Number ? ((Number) task.getInputData().get("model2Accuracy")).doubleValue() : 0;
        String best = acc1 > acc2 ? "random_forest" : "gradient_boost";
        System.out.println("  [evaluate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bestModel", best);
        result.getOutputData().put("bestAccuracy", Math.max(acc1, acc2));
        return result;
    }
}