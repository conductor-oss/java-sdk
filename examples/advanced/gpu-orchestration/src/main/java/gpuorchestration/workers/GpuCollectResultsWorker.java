package gpuorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GpuCollectResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gpu_collect_results";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [collect] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collected", true);
        result.getOutputData().put("artifacts", java.util.List.of("model.pt", "metrics.json"));
        return result;
    }
}