package gpuorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GpuCheckAvailabilityWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gpu_check_availability";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [check] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("available", 4);
        result.getOutputData().put("gpuType", task.getInputData().getOrDefault("gpuType", "A100"));
        result.getOutputData().put("cluster", "gpu-pool-1");
        return result;
    }
}