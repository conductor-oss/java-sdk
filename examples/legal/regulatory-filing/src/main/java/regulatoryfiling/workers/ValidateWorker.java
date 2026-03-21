package regulatoryfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {
    @Override public String getTaskDefName() { return "rgf_validate"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Validating filing package");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validated", true);
        result.getOutputData().put("errors", java.util.List.of());
        return result;
    }
}
