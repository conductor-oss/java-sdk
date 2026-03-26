package normalizer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NrmOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nrm_output";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [output] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("normalized", true);
        result.getOutputData().put("originalFormat", task.getInputData().getOrDefault("detectedFormat", "unknown"));
        result.getOutputData().put("outputFormat", "canonical");
        return result;
    }
}