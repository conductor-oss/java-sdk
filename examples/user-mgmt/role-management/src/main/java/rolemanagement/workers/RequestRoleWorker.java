package rolemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.UUID;

public class RequestRoleWorker implements Worker {
    @Override public String getTaskDefName() { return "rom_request_role"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [request] Role \"" + task.getInputData().get("role") + "\" requested for " + task.getInputData().get("userId") + " by " + task.getInputData().get("requestedBy"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", "RR-" + UUID.randomUUID().toString().substring(0, 8));
        result.getOutputData().put("logged", true);
        return result;
    }
}
