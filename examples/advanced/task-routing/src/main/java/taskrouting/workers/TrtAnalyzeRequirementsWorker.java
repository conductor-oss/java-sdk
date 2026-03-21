package taskrouting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrtAnalyzeRequirementsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "trt_analyze_requirements";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [analyze] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requirements", java.util.Map.of("cpu", "high", "memory", "16GB", "gpu", true));
        result.getOutputData().put("availablePools", java.util.List.of(java.util.Map.of("name", "gpu-pool-us", "capabilities", java.util.List.of("gpu", "high-cpu"), "region", "us-east", "load", 0.6)));
        return result;
    }
}