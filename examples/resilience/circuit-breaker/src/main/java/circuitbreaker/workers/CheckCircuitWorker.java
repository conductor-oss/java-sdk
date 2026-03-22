package circuitbreaker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for cb_check_circuit -- determines the circuit breaker state.
 *
 * Uses persistent state from CircuitBreakerState. Circuit breaker states:
 *   CLOSED    -- normal operation, calls go through
 *   OPEN      -- service is failing, calls are blocked
 *   HALF_OPEN -- testing if service has recovered
 *
 * Logic:
 * 1. If circuitState input is "OPEN", persist and return OPEN.
 * 2. If circuitState input is "HALF_OPEN", persist and return HALF_OPEN.
 * 3. Otherwise, if failureCount >= threshold, transition to OPEN.
 * 4. Otherwise, return CLOSED.
 *
 * Input:
 *   - serviceName (String, required): name of the service
 *   - circuitState (String, optional): forced state override
 *   - failureCount (int, optional): current failure count (default 0)
 *   - threshold (int, optional): failure threshold to trip breaker (default 3, must be > 0)
 *
 * Output:
 *   - state (String): resolved circuit state
 *   - failureCount (int): current failure count
 *   - threshold (int): configured threshold
 */
public class CheckCircuitWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cb_check_circuit";
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        // Validate serviceName
        String serviceName = getStringInput(task, "serviceName");
        if (serviceName == null || serviceName.isBlank()) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Missing required input: serviceName");
            return result;
        }

        String circuitState = getStringInput(task, "circuitState");
        int failureCount = getIntInput(task, "failureCount", 0);
        int threshold = getIntInput(task, "threshold", 3);

        // Validate threshold
        if (threshold <= 0) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid threshold: must be > 0, got " + threshold);
            return result;
        }

        // Validate failureCount
        if (failureCount < 0) {
            result.setStatus(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR);
            result.setReasonForIncompletion("Invalid failureCount: must be >= 0, got " + failureCount);
            return result;
        }

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

        // Persist state
        CircuitBreakerState.setState(serviceName, state);

        System.out.println("  [cb_check_circuit] service=" + serviceName
                + " circuitState=" + circuitState
                + " failureCount=" + failureCount + " threshold=" + threshold
                + " => " + state);

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("state", state);
        result.getOutputData().put("failureCount", failureCount);
        result.getOutputData().put("threshold", threshold);
        result.getOutputData().put("serviceName", serviceName);
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
