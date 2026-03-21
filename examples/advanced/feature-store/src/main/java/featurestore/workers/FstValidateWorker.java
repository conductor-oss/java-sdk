package featurestore.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class FstValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fst_validate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [validate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("validFeatures", task.getInputData().get("features"));
        result.getOutputData().put("version", "v1.0");
        return result;
    }
}