package gracefulserviceshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Deregisters the service instance from the service registry.
 * Input: serviceName, instanceId
 * Output: deregistered (boolean), serviceName, instanceId
 */
public class DeregisterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gs_deregister";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) {
            serviceName = "unknown-service";
        }

        String instanceId = (String) task.getInputData().get("instanceId");
        if (instanceId == null) {
            instanceId = "unknown-instance";
        }

        System.out.println("  [deregister] " + instanceId + " removed from " + serviceName + " registry");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("deregistered", true);
        result.getOutputData().put("serviceName", serviceName);
        result.getOutputData().put("instanceId", instanceId);
        return result;
    }
}
