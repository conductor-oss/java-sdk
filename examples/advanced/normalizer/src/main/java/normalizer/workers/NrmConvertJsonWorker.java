package normalizer.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class NrmConvertJsonWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nrm_convert_json";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [convert-json] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("canonical", java.util.Map.of("format", "canonical", "source", "json", "data", task.getInputData().getOrDefault("rawInput", "")));
        return result;
    }
}