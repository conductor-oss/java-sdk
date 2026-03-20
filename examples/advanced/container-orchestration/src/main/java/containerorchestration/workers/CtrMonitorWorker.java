package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Enables monitoring for a deployment.
 * Input: serviceName, deploymentId
 * Output: enabled, dashboardUrl
 */
public class CtrMonitorWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctr_monitor";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown";
        String deploymentId = (String) task.getInputData().get("deploymentId");

        System.out.println("  [monitor] Monitoring enabled for deployment " + deploymentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enabled", true);
        result.getOutputData().put("dashboardUrl", "https://grafana.io/d/" + serviceName);
        return result;
    }
}
