package workflowoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfoParallelizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfo_parallelize";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [parallelize] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("optimizedPlan", java.util.Map.of("stages", java.util.List.of(java.util.Map.of("stage", 1, "tasks", java.util.List.of("validate")), java.util.Map.of("stage", 2, "tasks", java.util.List.of("enrich", "transform"))), "estimatedDurationMs", 750));
        return result;
    }
}