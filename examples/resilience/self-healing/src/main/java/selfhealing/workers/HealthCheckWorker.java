package selfhealing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for sh_health_check — checks the health of a service.
 *
 * If service is "broken-service", returns healthy="false" with symptoms.
 * Otherwise returns healthy="true".
 *
 * IMPORTANT: healthy is returned as a String ("true"/"false"), not a boolean,
 * because the SWITCH task evaluates value-param comparisons as strings.
 */
public class HealthCheckWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sh_health_check";
    }

    @Override
    public TaskResult execute(Task task) {
        String service = "";
        Object serviceInput = task.getInputData().get("service");
        if (serviceInput != null) {
            service = serviceInput.toString();
        }

        System.out.println("  [sh_health_check] Checking health of service: " + service);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        if ("broken-service".equals(service)) {
            result.getOutputData().put("healthy", "false");
            result.getOutputData().put("symptoms", "connection_timeouts");
            result.getOutputData().put("service", service);
            System.out.println("  [sh_health_check] Service unhealthy: connection_timeouts");
        } else {
            result.getOutputData().put("healthy", "true");
            result.getOutputData().put("service", service);
            System.out.println("  [sh_health_check] Service healthy");
        }

        return result;
    }
}
