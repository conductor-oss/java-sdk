package circuitbreaker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for cb_check_circuit -- determines the circuit breaker state.
 *
 * Circuit breaker states:
 *   CLOSED  -- normal operation, calls go through
 *   OPEN    -- service is failing, calls are blocked
 *   HALF_OPEN -- testing if service has recovered
 *
 * Logic:
 * 1. If circuitState input is "OPEN", return OPEN (forced open).
 * 2. If circuitState input is "HALF_OPEN", return HALF_OPEN (forced half-open).
 * 3. Otherwise, if failureCount >= threshold, return OPEN.
 * 4. Otherwise, return CLOSED.
 */
public class CheckCircuitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cb_check_circuit";
    }

    @Override
    public TaskResult execute(Task task) {
        String circuitState = getStringInput(task, "circuitState");
        int failureCount = getIntInput(task, "failureCount", 0);
        int threshold = getIntInput(task, "threshold", 3);

        String state;

        if ("OPEN".equals(circuitState)) {
            state = "OPEN";
        } else if ("HALF_OPEN".equals(circuitState)) {
            state = "HALF_OPEN";
        } else if (failureCount >= threshold) {
            state = "OPEN";
        } else {
            state = "CLOSED";
        }

        System.out.println("  [cb_check_circuit] circuitState=" + circuitState
                + " failureCount=" + failureCount + " threshold=" + threshold
                + " => " + state);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("state", state);
        result.getOutputData().put("failureCount", failureCount);
        result.getOutputData().put("threshold", threshold);
        return result;
    }

    private String getStringInput(Task task, String key) {
        Object value = task.getInputData().get(key);
        return value != null ? value.toString() : null;
    }

    private int getIntInput(Task task, String key, int defaultValue) {
        Object value = task.getInputData().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
