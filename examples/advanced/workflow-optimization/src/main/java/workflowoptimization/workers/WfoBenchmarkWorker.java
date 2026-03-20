package workflowoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfoBenchmarkWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfo_benchmark";
    }

    @Override
    public TaskResult execute(Task task) {
        int original = task.getInputData().get("originalDurationMs") instanceof Number ? ((Number) task.getInputData().get("originalDurationMs")).intValue() : 1050;
        System.out.println("  [benchmark] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("benchmarkResult", java.util.Map.of("originalMs", original, "optimizedMs", 750, "improvementPercent", 28.6, "verdict", "optimization_beneficial"));
        return result;
    }
}