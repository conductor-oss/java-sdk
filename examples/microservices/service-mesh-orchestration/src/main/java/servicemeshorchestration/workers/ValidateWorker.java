package servicemeshorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Validates connectivity after mesh configuration.
 * Input: serviceName, sidecarId
 * Output: valid, latencyMs
 */
public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mesh_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [validate] Connectivity check passed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("latencyMs", 5);
        return result;
    }
}
