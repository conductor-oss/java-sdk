package circuitbreaker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for cb_call_service -- performs the normal service call.
 *
 * Called when the circuit is CLOSED (or HALF_OPEN for testing).
 *
 * Input:
 *   - serviceName (String, required): name of the service to call
 *   - shouldFail (boolean, optional): if true, simulates service failure
 *
 * Output:
 *   - result (String): service response
 *   - source (String): "live"
 *   - serviceName (String): name of the service called
 */
public class CallServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cb_call_service";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        String serviceName = getStringInput(task, "serviceName");
        if (serviceName == null || serviceName.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: serviceName");
            return result;
        }

        Object shouldFailObj = task.getInputData().get("shouldFail");
        boolean shouldFail = Boolean.TRUE.equals(shouldFailObj)
                || "true".equals(String.valueOf(shouldFailObj));

        if (shouldFail) {
            // Increment failure count in persistent state
            int newCount = CircuitBreakerState.incrementFailureCount(serviceName);
            System.out.println("  [cb_call_service] Service " + serviceName + " FAILED (failures=" + newCount + ")");

            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Service " + serviceName + " call failed");
            result.getOutputData().put("serviceName", serviceName);
            result.getOutputData().put("failureCount", newCount);
            return result;
        }

        // Success: reset failure count
        CircuitBreakerState.resetFailureCount(serviceName);

        System.out.println("  [cb_call_service] Calling service: " + serviceName);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "Service " + serviceName + " responded successfully");
        result.getOutputData().put("source", "live");
        result.getOutputData().put("serviceName", serviceName);
        return result;
    }

    private String getStringInput(Task task, String key) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : null;
    }
}
