package circuitbreaker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for cb_fallback -- returns cached/fallback data.
 *
 * Called when the circuit is OPEN (service is failing).
 * Returns a fallback result instead of calling the actual service.
 */
public class FallbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cb_fallback";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = getStringInput(task, "serviceName", "default-service");

        System.out.println("  [cb_fallback] Returning fallback data for service: " + serviceName);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "Fallback data for " + serviceName);
        result.getOutputData().put("source", "cache");
        result.getOutputData().put("serviceName", serviceName);
        return result;
    }

    private String getStringInput(Task task, String key, String defaultValue) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
