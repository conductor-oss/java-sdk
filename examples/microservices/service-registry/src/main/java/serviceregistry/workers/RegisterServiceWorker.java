package serviceregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Registers a service in the service registry.
 * Input: serviceName, serviceUrl, version
 * Output: registrationId, registered
 */
public class RegisterServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_register_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        String serviceUrl = (String) task.getInputData().get("serviceUrl");
        if (serviceName == null) {
            serviceName = "unknown";
        }
        if (serviceUrl == null) {
            serviceUrl = "unknown";
        }

        System.out.println("  [register] " + serviceName + " at " + serviceUrl);

        String regId = "REG-" + serviceName.hashCode();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registrationId", regId);
        result.getOutputData().put("registered", true);
        return result;
    }
}
