package workflowoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfoAnalyzeExecutionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfo_analyze_execution";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [analyze] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dependencyGraph", java.util.Map.of("validate", java.util.List.of(), "enrich", java.util.List.of("validate"), "transform", java.util.List.of("validate")));
        result.getOutputData().put("taskTimings", java.util.Map.of("validate", 100, "enrich", 300, "transform", 400));
        result.getOutputData().put("originalOrder", java.util.List.of("validate", "enrich", "transform", "persist", "notify"));
        result.getOutputData().put("totalDurationMs", 1050);
        return result;
    }
}