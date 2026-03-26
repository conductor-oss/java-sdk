package gracefulserviceshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Stops accepting new tasks for the given service instance.
 * Input: serviceName, instanceId
 * Output: stopped (boolean)
 */
public class StopAcceptingWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gs_stop_accepting";
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

        System.out.println("  [stop] " + instanceId + " no longer accepting new tasks");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("stopped", true);
        result.getOutputData().put("serviceName", serviceName);
        result.getOutputData().put("instanceId", instanceId);
        return result;
    }
}
