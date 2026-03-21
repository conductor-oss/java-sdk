package gpuorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GpuSubmitJobWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gpu_submit_job";
    }

    @Override
    public TaskResult execute(Task task) {
        String jobId = (String) task.getInputData().getOrDefault("jobId", "unknown");
        System.out.println("  [submit] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("outputPath", "/results/" + jobId + "/output.pt");
        result.getOutputData().put("epochs", 50);
        result.getOutputData().put("lossVal", 0.023);
        return result;
    }
}