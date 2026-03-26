package servicemeshorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Configures mutual TLS for a service.
 * Input: serviceName, sidecarId
 * Output: enabled, certExpiry
 */
public class ConfigureMtlsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mesh_configure_mtls";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown";

        System.out.println("  [mTLS] Configuring mutual TLS for " + serviceName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enabled", true);
        result.getOutputData().put("certExpiry", "2027-01-01");
        return result;
    }
}
