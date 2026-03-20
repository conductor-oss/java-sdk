package gpuorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GpuAllocateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gpu_allocate";
    }

    @Override
    public TaskResult execute(Task task) {
        String gpuId = "GPU-" + (int)(Math.random() * 1000);
        System.out.println("  [allocate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("gpuId", gpuId);
        result.getOutputData().put("allocated", true);
        result.getOutputData().put("memoryGb", 80);
        return result;
    }
}