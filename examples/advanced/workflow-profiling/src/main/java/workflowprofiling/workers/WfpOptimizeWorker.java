package workflowprofiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfpOptimizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfp_optimize";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [optimize] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("optimizations", java.util.List.of(java.util.Map.of("task", "persist", "suggestion", "Use batch writes"), java.util.Map.of("task", "transform", "suggestion", "Parallelize transformations")));
        result.getOutputData().put("expectedSpeedup", "54% faster (2033ms -> 933ms)");
        return result;
    }
}