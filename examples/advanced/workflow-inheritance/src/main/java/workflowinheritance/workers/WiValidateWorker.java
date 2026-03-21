package workflowinheritance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WiValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wi_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        Object data = task.getInputData().getOrDefault("data", "");
        System.out.println("  [validate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedData", data);
        result.getOutputData().put("valid", true);
        return result;
    }
}