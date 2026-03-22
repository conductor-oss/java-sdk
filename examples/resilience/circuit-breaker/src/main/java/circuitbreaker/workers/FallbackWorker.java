package circuitbreaker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for cb_fallback -- returns cached/fallback data.
 *
 * Called when the circuit is OPEN (service is failing).
 *
 * Input:
 *   - serviceName (String, required): name of the service to provide fallback for
 *
 * Output:
 *   - result (String): fallback data
 *   - source (String): "cache"
 *   - serviceName (String): name of the service
 */
public class FallbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cb_fallback";
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

        System.out.println("  [cb_fallback] Returning fallback data for service: " + serviceName);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "Fallback data for " + serviceName);
        result.getOutputData().put("source", "cache");
        result.getOutputData().put("serviceName", serviceName);
        return result;
    }

    private String getStringInput(Task task, String key) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : null;
    }
}
