package circuitbreaker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for cb_call_service -- performs the normal service call.
 *
 * Called when the circuit is CLOSED (or HALF_OPEN for testing).
 * Returns a successful result with the service name and source.
 */
public class CallServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cb_call_service";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = getStringInput(task, "serviceName", "default-service");

        System.out.println("  [cb_call_service] Calling service: " + serviceName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "Service " + serviceName + " responded successfully");
        result.getOutputData().put("source", "live");
        result.getOutputData().put("serviceName", serviceName);
        return result;
    }

    private String getStringInput(Task task, String key, String defaultValue) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
