package gpuorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GpuReleaseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gpu_release";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [release] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("released", true);
        return result;
    }
}