package rolemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ValidateRoleWorker implements Worker {
    @Override public String getTaskDefName() { return "rom_validate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [validate] Role \"" + task.getInputData().get("role") + "\" approved for " + task.getInputData().get("userId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", true);
        result.getOutputData().put("conflictingRoles", List.of());
        return result;
    }
}
