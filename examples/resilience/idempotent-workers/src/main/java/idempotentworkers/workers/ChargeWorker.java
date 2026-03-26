package idempotentworkers.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotent charge worker — check-before-act pattern with a Map cache.
 *
 * Uses orderId as the idempotency key. On the first call for a given orderId,
 * processes the charge and caches the result. On subsequent calls with the same
 * orderId, returns the cached result without reprocessing.
 */
public class ChargeWorker implements Worker {

    private final ConcurrentHashMap<String, Map<String, Object>> chargeCache = new ConcurrentHashMap<>();

    @Override
    public String getTaskDefName() {
        return "idem_charge";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = (String) task.getInputData().get("orderId");
        Object amountInput = task.getInputData().get("amount");
        double amount = 0.0;
        if (amountInput instanceof Number) {
            amount = ((Number) amountInput).doubleValue();
        }

        TaskResult result = new TaskResult(task);

        // Check if this orderId has already been processed
        Map<String, Object> cached = chargeCache.get(orderId);
        if (cached != null) {
            System.out.println("  [idem_charge] Duplicate charge detected for order " + orderId + " — returning cached result");
            result.getOutputData().putAll(cached);
            result.setStatus(TaskResult.Status.COMPLETED);
            return result;
        }

        // Process the charge
        System.out.println("  [idem_charge] Processing charge for order " + orderId + ", amount=" + amount);

        Map<String, Object> output = new ConcurrentHashMap<>();
        output.put("charged", true);
        output.put("orderId", orderId);
        output.put("amount", amount);
        output.put("duplicate", false);

        // Cache the result
        chargeCache.put(orderId, output);

        result.getOutputData().putAll(output);
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

    /**
     * Clears the charge cache. Useful for testing.
     */
    public void clearCache() {
        chargeCache.clear();
    }
}
