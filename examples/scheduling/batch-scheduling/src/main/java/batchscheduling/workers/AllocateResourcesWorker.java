package batchscheduling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Allocates compute resources for the batch execution.
 * Input: batchId, orderedJobs, maxConcurrency
 * Output: allocation, resourcesAllocated, estimatedDurationMs
 */
public class AllocateResourcesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bs_allocate_resources";
    }

    @Override
    public TaskResult execute(Task task) {
        String batchId = (String) task.getInputData().get("batchId");
        Object maxConcurrency = task.getInputData().get("maxConcurrency");

        System.out.println("  [allocate] Allocating resources for batch " + batchId
                + " (max concurrency: " + maxConcurrency + ")...");

        Map<String, Object> allocation = Map.of(
                "cpuCores", 8,
                "memoryGb", 16,
                "workers", 4);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allocation", allocation);
        result.getOutputData().put("resourcesAllocated", true);
        result.getOutputData().put("estimatedDurationMs", 12000);
        return result;
    }
}
