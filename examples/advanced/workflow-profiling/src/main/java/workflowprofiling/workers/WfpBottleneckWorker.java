package workflowprofiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WfpBottleneckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfp_bottleneck";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [bottleneck] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bottlenecks", java.util.List.of(java.util.Map.of("task", "persist", "avgMs", 1115, "severity", "critical"), java.util.Map.of("task", "transform", "avgMs", 830, "severity", "high")));
        return result;
    }
}